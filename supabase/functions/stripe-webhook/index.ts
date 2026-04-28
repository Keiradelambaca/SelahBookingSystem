// Follow this setup guide to integrate the Deno language server with your editor:
// https://deno.land/manual/getting_started/setup_your_environment
// This enables autocomplete, go to definition, etc.

// Setup type definitions for built-in Supabase Runtime APIs
// supabase/functions/stripe-webhook/index.ts
import { serve } from "https://deno.land/std@0.224.0/http/server.ts";
import Stripe from "npm:stripe";

function text(status: number, body: string) {
  return new Response(body, {
    status,
    headers: { "Content-Type": "text/plain" },
  });
}

const STRIPE_SECRET_KEY = Deno.env.get("STRIPE_SECRET_KEY") ?? "";
const STRIPE_WEBHOOK_SECRET = Deno.env.get("STRIPE_WEBHOOK_SECRET") ?? "";
const SUPABASE_URL = Deno.env.get("SUPABASE_URL") ?? "";
const SUPABASE_SERVICE_ROLE_KEY =
  Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";

const stripe = new Stripe(STRIPE_SECRET_KEY, {
  apiVersion: "2026-04-22.dahlia",
});

serve(async (req) => {
  try {
    if (!STRIPE_SECRET_KEY || !STRIPE_WEBHOOK_SECRET) {
      return text(500, "Missing Stripe secrets");
    }

    if (!SUPABASE_URL || !SUPABASE_SERVICE_ROLE_KEY) {
      return text(500, "Missing Supabase secrets");
    }

    if (req.method !== "POST") {
      return text(405, "Method not allowed");
    }

    const sig = req.headers.get("stripe-signature");
    if (!sig) {
      return text(400, "Missing stripe-signature");
    }

    const rawBody = await req.text();

    let event: Stripe.Event;

    try {
      event = await stripe.webhooks.constructEventAsync(
        rawBody,
        sig,
        STRIPE_WEBHOOK_SECRET
      );
    } catch (err: any) {
      return text(
        400,
        `Webhook signature verification failed: ${String(err?.message ?? err)}`
      );
    }

    if (event.type !== "checkout.session.completed") {
      return text(200, "ignored");
    }

    const session = event.data.object as Stripe.Checkout.Session;

    const bookingId =
      session.metadata?.booking_id ||
      (typeof session.client_reference_id === "string"
        ? session.client_reference_id
        : "");

    const paymentType = session.metadata?.payment_type || "deposit";

    if (!bookingId) {
      return text(200, "no booking_id ignored");
    }

    let updateBody: Record<string, unknown>;

    if (paymentType === "balance") {
      updateBody = {
        payment_status: "fully_paid",
        status: "confirmed",
        payment_provider: "stripe",
        payment_ref: session.id,
        balance_paid_at: new Date().toISOString(),
      };
    } else {
      updateBody = {
        payment_status: "paid",
        status: "confirmed",
        payment_provider: "stripe",
        payment_ref: session.id,
        deposit_paid_at: new Date().toISOString(),
      };
    }

    const updateResp = await fetch(
      `${SUPABASE_URL}/rest/v1/bookings?id=eq.${encodeURIComponent(bookingId)}`,
      {
        method: "PATCH",
        headers: {
          "Content-Type": "application/json",
          apikey: SUPABASE_SERVICE_ROLE_KEY,
          Authorization: `Bearer ${SUPABASE_SERVICE_ROLE_KEY}`,
          Prefer: "return=minimal",
        },
        body: JSON.stringify(updateBody),
      }
    );

    if (!updateResp.ok) {
      const errText = await updateResp.text();
      return text(
        500,
        `Failed to update booking: ${updateResp.status} ${errText}`
      );
    }

    return text(200, `ok ${paymentType}`);
  } catch (e: any) {
    return text(500, String(e?.message ?? e));
  }
});

/* To invoke locally:

  1. Run `supabase start` (see: https://supabase.com/docs/reference/cli/supabase-start)
  2. Make an HTTP request:

  curl -i --location --request POST 'http://127.0.0.1:54321/functions/v1/stripe-webhook' \
    --header 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZS1kZW1vIiwicm9sZSI6ImFub24iLCJleHAiOjE5ODM4MTI5OTZ9.CRXP1A7WOeoJeXxjNni43kdQwgnWNReilDMblYTn_I0' \
    --header 'Content-Type: application/json' \
    --data '{"name":"Functions"}'

*/
