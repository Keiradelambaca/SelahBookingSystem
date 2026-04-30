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

type FinalAssessment = {
  assessment_id: string;
  analysis: {
    base_service_code: string;
    addon_codes: string[];
    design_level: "low" | "medium" | "high";
    shape: "round" | "square" | "almond" | "coffin" | "stiletto";
    length: "short" | "medium" | "long" | "xl";
    confidence_score: number;
    notes: string[];
  };
  recommendation: {
    total_duration_mins: number;
    total_price_cents: number;
  };
  slot_suggestions: string[];
};

async function validateImageUrl(label: string, url: string) {
  const res = await fetch(url);
  const contentType = res.headers.get("content-type") || "";

  console.log(`${label} url:`, url);
  console.log(`${label} status:`, res.status);
  console.log(`${label} content-type:`, contentType);

  if (!res.ok) {
    const preview = await res.text();
    throw new Error(
      `${label} fetch failed with status ${res.status}. Body preview: ${preview.slice(0, 300)}`
    );
  }

  if (!contentType.startsWith("image/")) {
    const preview = await res.text();
    throw new Error(
      `${label} did not return an image. content-type=${contentType}. Body preview: ${preview.slice(0, 300)}`
    );
  }
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

    const openAiApiKey = Deno.env.get("OPENAI_API_KEY");
    if (!openAiApiKey) {
      return jsonResponse(500, { error: "Missing OPENAI_API_KEY" });
    }

    const body = await req.json();
    const providerId = body?.provider_id;
    const currentPhotoUrl = body?.current_photo_url;
    const inspoPhotoUrl = body?.inspo_photo_url;

    if (!providerId || !currentPhotoUrl) {
      return jsonResponse(400, {
        error: "Missing required fields: provider_id, current_photo_url"
      });
    }

    const currentPhotoUrlStr = String(currentPhotoUrl).trim();
    const desiredPhotoUrl =
      inspoPhotoUrl && String(inspoPhotoUrl).trim().length > 0
        ? String(inspoPhotoUrl).trim()
        : currentPhotoUrlStr;

    console.log("analyse-nail-booking providerId:", providerId);
    console.log("analyse-nail-booking currentPhotoUrl:", currentPhotoUrlStr);
    console.log("analyse-nail-booking desiredPhotoUrl:", desiredPhotoUrl);

    await validateImageUrl("currentPhotoUrl", currentPhotoUrlStr);
    await validateImageUrl("desiredPhotoUrl", desiredPhotoUrl);

    const prompt = `
You are a nail-service recommendation assistant for a salon booking app.

You will receive:
- Image A: the customer's current nails
- Image B: the customer's desired nails or inspiration image

Your job is to decide the most likely service setup needed to get from image A to image B.

Return ONLY JSON matching the schema.

Rules:
- base_service_code must be exactly one of:
  gel_polish, biab_overlay, biab_infill, gel_extensions, acrylic_extensions, acrylic_infill

- addon_codes can contain zero or more of:
  removal, french_tip, chrome, ombre, gems_charms, hand_drawn_art, three_d_art, glitter, extra_length

- design_level must be one of:
  low, medium, high

- shape must be one of:
  round, square, almond, coffin, stiletto

- length must be one of:
  short, medium, long, xl

Guidance:
- If current nails visibly already have product and the desired look suggests replacing or rebuilding, include "removal".
- If the desired look is a French manicure, include "french_tip".
- If the desired nails are visibly very long, use length "xl" and include "extra_length".
- Be conservative and realistic.
- Prefer the single most likely base service.
- notes should be short and useful for UI/debugging.
- total_duration_mins and total_price_cents are provisional estimates only.
- If current nails mainly look grown out and the structure is still usable, an infill may be more likely than a full new set.
- If the current nails are short and the desired set is much longer, extensions are more likely.
- If the current shape or structure makes the target look unrealistic without rebuilding, include "removal".
`.trim();

    const schema = {
      type: "object",
      additionalProperties: false,
      properties: {
        assessment_id: { type: "string" },
        analysis: {
          type: "object",
          additionalProperties: false,
          properties: {
            base_service_code: {
              type: "string",
              enum: [
                "gel_polish",
                "biab_overlay",
                "biab_infill",
                "gel_extensions",
                "acrylic_extensions",
                "acrylic_infill"
              ]
            },
            addon_codes: {
              type: "array",
              items: {
                type: "string",
                enum: [
                  "removal",
                  "french_tip",
                  "chrome",
                  "ombre",
                  "gems_charms",
                  "hand_drawn_art",
                  "three_d_art",
                  "glitter",
                  "extra_length"
                ]
              }
            },
            design_level: {
              type: "string",
              enum: ["low", "medium", "high"]
            },
            shape: {
              type: "string",
              enum: ["round", "square", "almond", "coffin", "stiletto"]
            },
            length: {
              type: "string",
              enum: ["short", "medium", "long", "xl"]
            },
            confidence_score: {
              type: "number"
            },
            notes: {
              type: "array",
              items: { type: "string" }
            }
          },
          required: [
            "base_service_code",
            "addon_codes",
            "design_level",
            "shape",
            "length",
            "confidence_score",
            "notes"
          ]
        },
        recommendation: {
          type: "object",
          additionalProperties: false,
          properties: {
            total_duration_mins: { type: "integer" },
            total_price_cents: { type: "integer" }
          },
          required: ["total_duration_mins", "total_price_cents"]
        },
        slot_suggestions: {
          type: "array",
          items: { type: "string" }
        }
      },
      required: ["assessment_id", "analysis", "recommendation", "slot_suggestions"]
    };

    const openAiRes = await fetch("https://api.openai.com/v1/responses", {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${openAiApiKey}`,
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        model: "gpt-5.4-mini",
        input: [
          {
            role: "system",
            content: [
              {
                type: "input_text",
                text: "You analyse nail photos and output compact booking JSON."
              }
            ]
          },
          {
            role: "user",
            content: [
              {
                type: "input_text",
                text: prompt
              },
              {
                type: "input_text",
                text: "Image A: customer's current nails"
              },
              {
                type: "input_image",
                image_url: currentPhotoUrlStr,
                detail: "high"
              },
              {
                type: "input_text",
                text: "Image B: customer's desired or inspiration nails"
              },
              {
                type: "input_image",
                image_url: desiredPhotoUrl,
                detail: "high"
              }
            ]
          }
        ],
        text: {
          format: {
            type: "json_schema",
            name: "nail_booking_assessment",
            strict: true,
            schema
          }
        }
      })
    });

    const raw = await openAiRes.json();

    if (!openAiRes.ok) {
      console.error("OpenAI error:", raw);
      return jsonResponse(502, {
        error: "OpenAI request failed",
        details: raw
      });
    }

    const outputText =
      raw?.output?.[0]?.content?.find((c: any) => c.type === "output_text")?.text ??
      raw?.output_text;

    if (!outputText) {
      console.error("Missing output text:", raw);
      return jsonResponse(502, {
        error: "OpenAI returned no structured output",
        details: raw
      });
    }

    const parsed = JSON.parse(outputText) as FinalAssessment;

    parsed.assessment_id = parsed.assessment_id || crypto.randomUUID();
    parsed.recommendation = parsed.recommendation || {
      total_duration_mins: 0,
      total_price_cents: 0
    };
    parsed.slot_suggestions = parsed.slot_suggestions || [];

    return jsonResponse(200, parsed);
  } catch (e) {
    console.error("analyse-nail-booking error", e);
    return jsonResponse(500, {
      error: "Internal server error",
      details: e instanceof Error ? e.message : String(e)
    });
  }
});