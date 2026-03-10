package com.example.selahbookingsystem.adapter;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.selahbookingsystem.R;
import com.example.selahbookingsystem.data.dto.EmailTemplateDto;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmailTemplatesAdapter extends RecyclerView.Adapter<EmailTemplatesAdapter.VH> {

    private final List<EmailTemplateDto> items;

    // Live edits stored here so scrolling doesn't lose text
    private final Map<String, Draft> draftsByType = new HashMap<>();

    public EmailTemplatesAdapter(List<EmailTemplateDto> items) {
        this.items = items;

        // Seed drafts
        for (EmailTemplateDto t : items) {
            if (t != null && t.type != null) {
                draftsByType.put(t.type, new Draft(t));
            }
        }

        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        EmailTemplateDto t = items.get(position);
        return (t != null && t.type != null) ? t.type.hashCode() : super.getItemId(position);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_email_template_item, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        EmailTemplateDto item = items.get(position);
        if (item == null) return;

        if (item.type != null && !draftsByType.containsKey(item.type)) {
            draftsByType.put(item.type, new Draft(item));
        }

        Draft draft = draftsByType.get(item.type);
        if (draft == null) {
            draft = new Draft(item);
            draftsByType.put(item.type, draft);
        }

        final Draft d = draft;

        h.tvType.setText(prettyType(item.type));

        if (h.subjectWatcher != null) h.etSubject.removeTextChangedListener(h.subjectWatcher);
        if (h.bodyWatcher != null) h.etBody.removeTextChangedListener(h.bodyWatcher);
        if (h.enabledListener != null) h.swEnabled.setOnCheckedChangeListener(null);

        // Bind values
        h.swEnabled.setChecked(d.isEnabled);
        h.etSubject.setText(d.subject);
        h.etBody.setText(d.body);

        // Re-attach listeners
        h.enabledListener = (CompoundButton buttonView, boolean isChecked) -> d.isEnabled = isChecked;
        h.swEnabled.setOnCheckedChangeListener(h.enabledListener);

        h.subjectWatcher = new SimpleTextWatcher(s -> d.subject = s);
        h.etSubject.addTextChangedListener(h.subjectWatcher);

        h.bodyWatcher = new SimpleTextWatcher(s -> d.body = s);
        h.etBody.addTextChangedListener(h.bodyWatcher);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * Called by SPAutoEmailsActivity on save:
     * Returns templates with current edited values.
     */
    public List<EmailTemplateDto> collectCurrentValues(String providerId) {
        List<EmailTemplateDto> out = new ArrayList<>();

        for (EmailTemplateDto original : items) {
            if (original == null) continue;

            Draft d = draftsByType.get(original.type);
            if (d == null) d = new Draft(original);

            EmailTemplateDto dto = new EmailTemplateDto();

            // keep ID if exists so upsert updates instead of creating duplicates
            dto.id = original.id;

            dto.provider_id = providerId;
            dto.type = original.type;

            dto.subject = (d.subject == null) ? "" : d.subject.trim();
            dto.body = (d.body == null) ? "" : d.body.trim();

            dto.is_enabled = d.isEnabled;

            out.add(dto);
        }

        return out;
    }

    private String prettyType(String type) {
        if ("BOOKING_CONFIRMATION".equals(type)) return "Booking confirmation";
        if ("CANCELLATION".equals(type)) return "Cancellation";
        if ("RESCHEDULED".equals(type)) return "Rescheduled";
        if ("REMINDER_24H".equals(type)) return "24h reminder";
        return type == null ? "" : type;
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvType;
        SwitchMaterial swEnabled;
        EditText etSubject;
        EditText etBody;

        TextWatcher subjectWatcher;
        TextWatcher bodyWatcher;
        CompoundButton.OnCheckedChangeListener enabledListener;

        VH(@NonNull View itemView) {
            super(itemView);
            tvType = itemView.findViewById(R.id.tvType);
            swEnabled = itemView.findViewById(R.id.swEnabled);
            etSubject = itemView.findViewById(R.id.etSubject);
            etBody = itemView.findViewById(R.id.etBody);
        }
    }

    static class Draft {
        String subject;
        String body;
        boolean isEnabled;

        Draft(EmailTemplateDto t) {
            this.subject = t.subject == null ? "" : t.subject;
            this.body = t.body == null ? "" : t.body;
            this.isEnabled = (t.is_enabled == null) ? true : t.is_enabled;
        }
    }

    static class SimpleTextWatcher implements TextWatcher {
        interface OnTextChanged { void onChanged(String s); }
        private final OnTextChanged cb;

        SimpleTextWatcher(OnTextChanged cb) {
            this.cb = cb;
        }

        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
            cb.onChanged(s == null ? "" : s.toString());
        }
        @Override public void afterTextChanged(Editable s) {}
    }
}