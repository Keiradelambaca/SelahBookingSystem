import { serve } from "https://deno.land/std@0.224.0/http/server.ts";

function jsonResponse(status: number, body: unknown) {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      "Content-Type": "application/json",
      "Access-Control-Allow-Origin": "*",
      "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
      "Access-Control-Allow-Methods": "POST, OPTIONS"
    }
  });
}

serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", {
      headers: {
        "Access-Control-Allow-Origin": "*",
        "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
        "Access-Control-Allow-Methods": "POST, OPTIONS"
      }
    });
  }

  try {
    if (req.method !== "POST") {
      return jsonResponse(405, { error: "Method not allowed" });
    }

    const body = await req.json();
    const providerId = body?.provider_id;
    const currentPhotoUrl = body?.current_photo_url;
    const inspoPhotoUrl = body?.inspo_photo_url;

    if (!providerId || !currentPhotoUrl || !inspoPhotoUrl) {
      return jsonResponse(400, {
        error: "Missing required fields: provider_id, current_photo_url, inspo_photo_url"
      });
    }

    const mockResponse = {
      assessment_id: crypto.randomUUID(),
      analysis: {
        current_set: {
          has_existing_product: true,
          likely_product_type: "gel",
          length: "medium",
          shape: "almond",
          condition: "grown_out"
        },
        desired_set: {
          target_style: "french",
          target_shape: "square",
          target_length: "long",
          design_complexity: "complex",
          has_nail_art: false,
          special_features: []
        },
        transformation: {
          needs_removal: true,
          removal_type: "soak_off",
          needs_new_set: true,
          needs_infill: false
        },
        confidence_score: 0.84,
        notes: [
          "Existing product appears visible on the current nail image",
          "Desired image suggests a simple french finish",
          "A removal step is likely needed before the new service"
        ]
      },
      recommendation: {
        recommended_services: [
          {
            service_code: "REMOVAL",
            service_name: "Gel Removal",
            duration_mins: 20,
            price_cents: 1500
          },
          {
            service_code: "FRENCH_ADDON",
            service_name: "French Finish",
            duration_mins: 15,
            price_cents: 1000
          },
          {
            service_code: "SHAPE_SQUARE",
            service_name: "Square Shape",
            duration_mins: 5,
            price_cents: 0
          }
        ],
        total_duration_mins: 100,
        total_price_cents: 6500
      },
      slot_suggestions: [
        "2026-03-21T10:00:00Z",
        "2026-03-20T13:30:00Z",
        "2026-03-20T11:00:00Z"
      ]
    };

    return jsonResponse(200, mockResponse);
  } catch (e) {
    console.error("analyze-nail-booking error", e);
    return jsonResponse(500, {
      error: "Internal server error",
      details: e instanceof Error ? e.message : String(e)
    });
  }
});
