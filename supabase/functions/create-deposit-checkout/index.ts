// Follow this setup guide to integrate the Deno language server with your editor:
// https://deno.land/manual/getting_started/setup_your_environment
// This enables autocomplete, go to definition, etc.

// Setup type definitions for built-in Supabase Runtime APIs
// supabase/functions/create-deposit-checkout/index.ts
import { serve } from "https://deno.land/std@0.224.0/http/server.ts";
import Stripe from "npm:stripe";

type Json = Record<string, unknown>;

function json(status: number, body: Json) {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      "Content-Type": "application/json",
      "Access-Control-Allow-Origin": "*",
      "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
    },
  });
}

function corsPreflight(req: Request) {
  if (req.method === "OPTIONS") {
    return new Response("ok", {
      headers: {
        "Access-Control-Allow-Origin": "*",
        "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
        "Access-Control-Allow-Methods": "POST, OPTIONS",
      },
    });
  }
  return null;
}

serve(async (req) => {
  const pre = corsPreflight(req);
  if (pre) return pre;

  try {
    const STRIPE_SECRET_KEY = Deno.env.get("STRIPE_SECRET_KEY")!;
    const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
    const SUPABASE_SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;

    if (!STRIPE_SECRET_KEY) return json(500, { error: "Missing STRIPE_SECRET_KEY" });
    if (!SUPABASE_URL) return json(500, { error: "Missing SUPABASE_URL" });
    if (!SUPABASE_SERVICE_ROLE_KEY) return json(500, { error: "Missing SUPABASE_SERVICE_ROLE_KEY" });

    const stripe = new Stripe(STRIPE_SECRET_KEY, { apiVersion: "2024-06-20" });

    const body = await req.json();
    const booking_id = String(body.booking_id ?? "");
    const provider_name = String(body.provider_name ?? "Selah Provider");
    const deposit_amount_cents = Number(body.deposit_amount_cents ?? 0);

    if (!booking_id) return json(400, { error: "booking_id required" });
    if (!deposit_amount_cents || deposit_amount_cents <= 0) {
      return json(400, { error: "deposit_amount_cents must be > 0" });
    }

    // 1) Create Checkout Session
    const session = await stripe.checkout.sessions.create({
      mode: "payment",
      payment_method_types: ["card"],
      line_items: [
        {
          price_data: {
            currency: "eur",
            product_data: { name: `Deposit for booking (${provider_name})` },
            unit_amount: deposit_amount_cents,
          },
          quantity: 1,
        },
      ],
      metadata: { booking_id },
      success_url: "https://example.com/success",
      cancel_url: "https://example.com/cancel",
    });

    // 2) Save session.id to booking.payment_ref
    const updateResp = await fetch(
      `${SUPABASE_URL}/rest/v1/bookings?id=eq.${encodeURIComponent(booking_id)}`,
      {
        method: "PATCH",
        headers: {
          "Content-Type": "application/json",
          "apikey": SUPABASE_SERVICE_ROLE_KEY,
          "Authorization": `Bearer ${SUPABASE_SERVICE_ROLE_KEY}`,
          "Prefer": "return=minimal",
        },
        body: JSON.stringify({
          payment_ref: session.id,
          payment_provider: "stripe",
        }),
      }
    );

    if (!updateResp.ok) {
      const err = await updateResp.text();
      return json(500, { error: "Failed to update booking with payment_ref", details: err });
    }

    return json(200, { url: session.url, session_id: session.id });
  } catch (e) {
    return json(500, { error: String(e?.message ?? e) });
  }
});

/* To invoke locally:

  1. Run `supabase start` (see: https://supabase.com/docs/reference/cli/supabase-start)
  2. Make an HTTP request:

  curl -i --location --request POST 'http://127.0.0.1:54321/functions/v1/create-deposit-checkout' \
    --header 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZS1kZW1vIiwicm9sZSI6ImFub24iLCJleHAiOjE5ODM4MTI5OTZ9.CRXP1A7WOeoJeXxjNni43kdQwgnWNReilDMblYTn_I0' \
    --header 'Content-Type: application/json' \
    --data '{"name":"Functions"}'

*/
