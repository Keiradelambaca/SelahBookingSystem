// Follow this setup guide to integrate the Deno language server with your editor:
// https://deno.land/manual/getting_started/setup_your_environment
// This enables autocomplete, go to definition, etc.

// Setup type definitions for built-in Supabase Runtime APIs
// supabase/functions/send-reminders-24h/index.ts
// Sends:
// 1. Client reminder emails ~24h before appointment (per booking, once) + marks reminder_24h_sent_at
// 2. Provider DAILY summary email (one per provider) listing tomorrow's appointments

import { serve } from "https://deno.land/std@0.224.0/http/server.ts";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const RESEND_API_KEY = Deno.env.get("RESEND_API_KEY")!;
const EMAIL_FROM = Deno.env.get("EMAIL_FROM")!;

function json(status: number, body: unknown) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

const sbHeaders = {
  apikey: SERVICE_ROLE_KEY,
  Authorization: `Bearer ${SERVICE_ROLE_KEY}`,
  "Content-Type": "application/json",
  Accept: "application/json",
};

async function sbGet(path: string) {
  const r = await fetch(`${SUPABASE_URL}/rest/v1/${path}`, { headers: sbHeaders });
  const txt = await r.text();
  if (!r.ok) throw new Error(`Supabase GET ${r.status}: ${txt}`);
  return txt ? JSON.parse(txt) : null;
}

async function sbPatch(path: string, body: unknown, prefer = "return=minimal") {
  const r = await fetch(`${SUPABASE_URL}/rest/v1/${path}`, {
    method: "PATCH",
    headers: { ...sbHeaders, Prefer: prefer },
    body: JSON.stringify(body),
  });
  const txt = await r.text();
  if (!r.ok) throw new Error(`Supabase PATCH ${r.status}: ${txt}`);
  return txt ? JSON.parse(txt) : null;
}

async function sbPost(path: string, body: unknown, prefer = "return=representation") {
  const r = await fetch(`${SUPABASE_URL}/rest/v1/${path}`, {
    method: "POST",
    headers: { ...sbHeaders, Prefer: prefer },
    body: JSON.stringify(body),
  });
  const txt = await r.text();
  if (!r.ok) throw new Error(`Supabase POST ${r.status}: ${txt}`);
  return txt ? JSON.parse(txt) : null;
}

async function sbRpc(fnName: string, body: unknown = {}) {
  const r = await fetch(`${SUPABASE_URL}/rest/v1/rpc/${fnName}`, {
    method: "POST",
    headers: sbHeaders,
    body: JSON.stringify(body),
  });
  const txt = await r.text();
  if (!r.ok) throw new Error(`Supabase RPC ${r.status}: ${txt}`);
  return txt ? JSON.parse(txt) : null;
}

function fmtLocal(iso: string) {
  const d = new Date(iso);
  const date = new Intl.DateTimeFormat("en-IE", {
    timeZone: "Europe/Dublin",
    weekday: "short",
    day: "2-digit",
    month: "short",
  }).format(d);
  const time = new Intl.DateTimeFormat("en-IE", {
    timeZone: "Europe/Dublin",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  }).format(d);
  return `${date} • ${time}`;
}

async function resendSend(to: string, subject: string, html: string) {
  const r = await fetch("https://api.resend.com/emails", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${RESEND_API_KEY}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      from: EMAIL_FROM,
      to: [to],
      subject,
      html,
    }),
  });

  const txt = await r.text();
  if (!r.ok) throw new Error(`Resend ${r.status}: ${txt}`);
}

function escapeHtml(s: string) {
  return (s ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

// YYYY-MM-DD for tomorrow in Europe/Dublin
function tomorrowDateDublin(): string {
  const now = new Date();
  const dTomorrow = new Date(now.getTime() + 24 * 60 * 60 * 1000);
  const parts = new Intl.DateTimeFormat("en-CA", {
    timeZone: "Europe/Dublin",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(dTomorrow);
  return parts;
}

serve(async (req) => {
  try {
    if (req.method !== "POST") return json(405, { error: "Use POST" });


    // 1.CLIENT reminders ~24h
    const now = new Date();
    const start = new Date(now.getTime() + 24 * 60 * 60 * 1000 - 5 * 60 * 1000); // 24h - 5m
    const end = new Date(now.getTime() + 24 * 60 * 60 * 1000 + 5 * 60 * 1000);   // 24h + 5m

    const clientReminderRows = await sbGet(
      `bookings?status=eq.confirmed` +
        `&start_time=gte.${encodeURIComponent(start.toISOString())}` +
        `&start_time=lt.${encodeURIComponent(end.toISOString())}` +
        `&reminder_24h_sent_at=is.null` +
        `&select=id,client_id,provider_id,start_time,end_time,provider_name`
    );

    let clientSent = 0;

    for (const b of (clientReminderRows || [])) {
      const clientRows = await sbGet(
        `profiles?id=eq.${encodeURIComponent(b.client_id)}&select=full_name,email`
      );
      const providerRows = await sbGet(
        `profiles?id=eq.${encodeURIComponent(b.provider_id)}&select=full_name,business_name,email`
      );

      if (!clientRows?.length || !providerRows?.length) continue;

      const client = clientRows[0];
      const provider = providerRows[0];
      const providerName = provider.business_name || provider.full_name || b.provider_name || "Provider";
      const clientName = client.full_name || "there";

      const subject = "Reminder: appointment tomorrow";
      const html = `
        <div style="font-family:Arial,sans-serif;line-height:1.4">
          <h2>Reminder</h2>
          <p>Hi ${escapeHtml(clientName)},</p>
          <p>This is a reminder that you have an appointment with <b>${escapeHtml(providerName)}</b> at:</p>
          <p><b>${escapeHtml(fmtLocal(b.start_time))}</b></p>
        </div>
      `;

      await resendSend(client.email, subject, html);

      // mark sent so it won't resend
      await sbPatch(`bookings?id=eq.${encodeURIComponent(b.id)}`, {
        reminder_24h_sent_at: new Date().toISOString(),
      });

      clientSent++;
    }


    // 2.PROVIDER daily summary for TOMORROW
    const reminderDate = tomorrowDateDublin();

    // Get all tomorrow confirmed bookings (Dublin calendar day)
    const tomorrowBookings = await sbRpc("get_tomorrow_confirmed_bookings", {});

    // Group by provider
    const byProvider = new Map<string, Array<any>>();
    for (const b of (tomorrowBookings || [])) {
      const arr = byProvider.get(b.provider_id) ?? [];
      arr.push(b);
      byProvider.set(b.provider_id, arr);
    }

    let providerSummariesSent = 0;

    for (const [providerId, bookings] of byProvider.entries()) {
      // skip if already sent summary for this provider+date
      const sentRows = await sbGet(
        `provider_daily_reminder_sent?provider_id=eq.${encodeURIComponent(providerId)}` +
          `&reminder_date=eq.${encodeURIComponent(reminderDate)}` +
          `&select=provider_id`
      );
      if (sentRows?.length) continue;

      // provider profile
      const providerRows = await sbGet(
        `profiles?id=eq.${encodeURIComponent(providerId)}&select=full_name,business_name,email`
      );
      if (!providerRows?.length) continue;

      const provider = providerRows[0];
      if (!provider.email) continue;

      const providerName = provider.business_name || provider.full_name || "Provider";

      // Build list items
      bookings.sort((a, b) => new Date(a.start_time).getTime() - new Date(b.start_time).getTime());

      const lines: string[] = [];
      for (const b of bookings) {
        const clientRows = await sbGet(
          `profiles?id=eq.${encodeURIComponent(b.client_id)}&select=full_name`
        );
        const clientName = clientRows?.[0]?.full_name || "Client";
        lines.push(`<li><b>${escapeHtml(fmtLocal(b.start_time))}</b> — ${escapeHtml(clientName)}</li>`);
      }

      const subject = `Tomorrow’s appointments (${reminderDate})`;
      const html = `
        <div style="font-family:Arial,sans-serif;line-height:1.4">
          <h2>Tomorrow’s appointments</h2>
          <p>Hi ${escapeHtml(providerName)}, here’s your schedule for <b>${escapeHtml(reminderDate)}</b>:</p>
          <ul>
            ${lines.join("\n")}
          </ul>
        </div>
      `;

      await resendSend(provider.email, subject, html);

      // record sent
      await sbPost("provider_daily_reminder_sent", {
        provider_id: providerId,
        reminder_date: reminderDate,
      }, "return=minimal");

      providerSummariesSent++;
    }

    return json(200, {
      ok: true,
      client_reminders_sent: clientSent,
      provider_daily_summaries_sent: providerSummariesSent,
      provider_summary_date: reminderDate,
      client_window: { start: start.toISOString(), end: end.toISOString() },
    });
  } catch (e) {
    return json(500, { error: String((e as Error)?.message || e) });
  }
});

/* To invoke locally:

  1. Run `supabase start` (see: https://supabase.com/docs/reference/cli/supabase-start)
  2. Make an HTTP request:

  curl -i --location --request POST 'http://127.0.0.1:54321/functions/v1/send-reminder-24h' \
    --header 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZS1kZW1vIiwicm9sZSI6ImFub24iLCJleHAiOjE5ODM4MTI5OTZ9.CRXP1A7WOeoJeXxjNni43kdQwgnWNReilDMblYTn_I0' \
    --header 'Content-Type: application/json' \
    --data '{"name":"Functions"}'

*/
