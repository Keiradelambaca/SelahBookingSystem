// Follow this setup guide to integrate the Deno language server with your editor:
// https://deno.land/manual/getting_started/setup_your_environment
// This enables autocomplete, go to definition, etc.

// Setup type definitions for built-in Supabase Runtime APIs
import { serve } from "https://deno.land/std@0.224.0/http/server.ts";
import Stripe from "https://esm.sh/stripe@14.25.0";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.45.4";

const stripe = new Stripe(Deno.env.get("STRIPE_SECRET_KEY") ?? "", {
  apiVersion: "2024-06-20",
});

const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";

serve(async (req) => {
  try {
    if (req.method !== "POST") {
      return new Response("Method not allowed", { status: 405 });
    }

    const authHeader = req.headers.get("Authorization") ?? "";

    const supabase = createClient(supabaseUrl, serviceRoleKey, {
      global: {
        headers: {
          Authorization: authHeader,
        },
      },
    });

    const {
      data: { user },
      error: userError,
    } = await supabase.auth.getUser(authHeader.replace("Bearer ", ""));

    if (userError || !user) {
      return Response.json({ error: "Unauthorised" }, { status: 401 });
    }

    const body = await req.json();
    const bookingId = body.booking_id;

    if (!bookingId) {
      return Response.json({ error: "Missing booking_id" }, { status: 400 });
    }

    const { data: booking, error: bookingError } = await supabase
      .from("bookings")
      .select("id, client_id, provider_name, total_price_cents, deposit_amount_cents, payment_status")
      .eq("id", bookingId)
      .single();

    if (bookingError || !booking) {
      return Response.json({ error: "Booking not found" }, { status: 404 });
    }

    if (booking.client_id !== user.id) {
      return Response.json({ error: "Forbidden" }, { status: 403 });
    }

    const total = booking.total_price_cents ?? 0;
    const deposit = booking.deposit_amount_cents ?? 0;

    let amountCents = total - deposit;

    if (
      booking.payment_status === "fully_paid" ||
      booking.payment_status === "paid_in_full"
    ) {
      return Response.json({ error: "Booking already fully paid" }, { status: 400 });
    }

    if (amountCents <= 0) {
      return Response.json({ error: "No remaining balance" }, { status: 400 });
    }

    const origin = req.headers.get("origin") ?? "https://example.com";

    const session = await stripe.checkout.sessions.create({
      mode: "payment",
      payment_method_types: ["card"],
      line_items: [
        {
          quantity: 1,
          price_data: {
            currency: "eur",
            unit_amount: amountCents,
            product_data: {
              name: `Remaining balance - ${booking.provider_name ?? "Appointment"}`,
            },
          },
        },
      ],
      metadata: {
        booking_id: bookingId,
        payment_type: "balance",
      },
      success_url: `${origin}/payment-success?booking_id=${bookingId}`,
      cancel_url: `${origin}/payment-cancelled?booking_id=${bookingId}`,
    });

    await supabase
      .from("bookings")
      .update({
        balance_amount_cents: amountCents,
        balance_payment_ref: session.id,
      })
      .eq("id", bookingId);

    return Response.json({
      url: session.url,
      session_id: session.id,
      amount_cents: amountCents,
    });
  } catch (err) {
    console.error(err);
    return Response.json(
      { error: err instanceof Error ? err.message : "Unknown error" },
      { status: 500 },
    );
  }
});

/* To invoke locally:

  1. Run `supabase start` (see: https://supabase.com/docs/reference/cli/supabase-start)
  2. Make an HTTP request:

  curl -i --location --request POST 'http://127.0.0.1:54321/functions/v1/create-blanace-checkout' \
    --header 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZS1kZW1vIiwicm9sZSI6ImFub24iLCJleHAiOjE5ODM4MTI5OTZ9.CRXP1A7WOeoJeXxjNni43kdQwgnWNReilDMblYTn_I0' \
    --header 'Content-Type: application/json' \
    --data '{"name":"Functions"}'

*/
