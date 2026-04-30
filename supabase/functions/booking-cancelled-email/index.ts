// Follow this setup guide to integrate the Deno language server with your editor:
// https://deno.land/manual/getting_started/setup_your_environment
// This enables autocomplete, go to definition, etc.

// Setup type definitions for built-in Supabase Runtime APIs
// supabase/functions/booking-cancelled-email/index.ts

import { serve } from "https://deno.land/std@0.224.0/http/server.ts";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const RESEND_API_KEY = Deno.env.get("RESEND_API_KEY")!;
const EMAIL_FROM = Deno.env.get("EMAIL_FROM")!;

serve(async (req) => {
  try {
    const { booking_id } = await req.json();
    if (!booking_id) return new Response("Missing booking_id", { status: 400 });

    const headers = {
      apikey: SERVICE_ROLE_KEY,
      Authorization: `Bearer ${SERVICE_ROLE_KEY}`,
    };

    const bookingRes = await fetch(
      `${SUPABASE_URL}/rest/v1/bookings?id=eq.${booking_id}&select=*`,
      { headers }
    );
    const booking = (await bookingRes.json())[0];

    const clientRes = await fetch(
      `${SUPABASE_URL}/rest/v1/profiles?id=eq.${booking.client_id}&select=full_name,email`,
      { headers }
    );
    const providerRes = await fetch(
      `${SUPABASE_URL}/rest/v1/profiles?id=eq.${booking.provider_id}&select=full_name,business_name,email`,
      { headers }
    );

    const client = (await clientRes.json())[0];
    const provider = (await providerRes.json())[0];

    const providerName =
      provider.business_name || provider.full_name || booking.provider_name;

    const startLocal = new Date(booking.start_time).toLocaleString("en-IE", {
      timeZone: "Europe/Dublin",
    });

    const html = `
      <div style="font-family:Arial,sans-serif">
        <h2>Appointment Cancelled</h2>
        <p>Hi ${client.full_name},</p>
        <p>Your appointment with <b>${providerName}</b> on:</p>
        <p><b>${startLocal}</b></p>
        <p>has been cancelled.</p>
      </div>
    `;

    await fetch("https://api.resend.com/emails", {
      method: "POST",
      headers: {
        Authorization: `Bearer ${RESEND_API_KEY}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        from: EMAIL_FROM,
        to: [client.email],
        subject: "Appointment Cancelled",
        html,
      }),
    });

    // Send copy to provider
    if (provider?.email) {
      const providerHtml = `
        <div style="font-family:Arial,sans-serif">
          <h2>Appointment Cancelled</h2>
          <p><b>Client:</b> ${client.full_name || "Client"}</p>
          <p><b>Time:</b> ${startLocal}</p>
          <p><b>Booking ID:</b> ${booking.id}</p>
        </div>
      `;

      await fetch("https://api.resend.com/emails", {
        method: "POST",
        headers: {
          Authorization: `Bearer ${RESEND_API_KEY}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          from: EMAIL_FROM,
          to: [provider.email],
          subject: `Cancellation: ${startLocal}`,
          html: providerHtml,
        }),
      });
    }

    return new Response(JSON.stringify({ ok: true }));
  } catch (e) {
    return new Response(JSON.stringify({ error: e.message }), { status: 500 });
  }
});

/* To invoke locally:

  1. Run `supabase start` (see: https://supabase.com/docs/reference/cli/supabase-start)
  2. Make an HTTP request:

  curl -i --location --request POST 'http://127.0.0.1:54321/functions/v1/booking-cancelled-email' \
    --header 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZS1kZW1vIiwicm9sZSI6ImFub24iLCJleHAiOjE5ODM4MTI5OTZ9.CRXP1A7WOeoJeXxjNni43kdQwgnWNReilDMblYTn_I0' \
    --header 'Content-Type: application/json' \
    --data '{"name":"Functions"}'

*/
