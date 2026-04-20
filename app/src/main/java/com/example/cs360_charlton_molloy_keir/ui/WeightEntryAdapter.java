package com.example.cs360_charlton_molloy_keir.ui;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cs360_charlton_molloy_keir.R;
import com.example.cs360_charlton_molloy_keir.databinding.ItemWeightEntryBinding;
import com.example.cs360_charlton_molloy_keir.model.WeightEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Binds weight-history entries to compact card rows with an overflow action menu */
public class WeightEntryAdapter
        extends ListAdapter<WeightEntry, WeightEntryAdapter.WeightEntryViewHolder> {

    private static final DiffUtil.ItemCallback<WeightEntry> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull WeightEntry oldItem,
                        @NonNull WeightEntry newItem
                ) {
                    return oldItem.id == newItem.id;
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull WeightEntry oldItem,
                        @NonNull WeightEntry newItem
                ) {
                    return oldItem.date.equals(newItem.date)
                            && Double.compare(oldItem.weight, newItem.weight) == 0;
                }
            };

    /** Receives edit and delete actions from individual rows */
    public interface EntryActionListener {
        void onEdit(WeightEntry entry);
        void onDelete(WeightEntry entry);
    }

    private final EntryActionListener entryActionListener;

    public WeightEntryAdapter(EntryActionListener entryActionListener) {
        super(DIFF_CALLBACK);
        this.entryActionListener = entryActionListener;
        setHasStableIds(true);
    }

    public void submitEntries(List<WeightEntry> entries) {
        submitList(new ArrayList<>(entries));
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).id;
    }

    @NonNull
    @Override
    public WeightEntryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        ItemWeightEntryBinding binding =
                ItemWeightEntryBinding.inflate(layoutInflater, parent, false);
        return new WeightEntryViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull WeightEntryViewHolder holder, int position) {
        holder.bind(getItem(position), entryActionListener);
    }

    public static final class WeightEntryViewHolder extends RecyclerView.ViewHolder {
        private final ItemWeightEntryBinding binding;

        WeightEntryViewHolder(ItemWeightEntryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(WeightEntry entry, EntryActionListener entryActionListener) {
            binding.textEntryDate.setText(entry.date);
            binding.textEntryWeight.setText(
                    binding.getRoot().getContext().getString(
                            R.string.analytics_weight_value,
                            String.format(Locale.US, "%.1f", entry.weight)
                    )
            );

            binding.buttonEntryMenu.setOnClickListener(v -> {
                PopupMenu popupMenu = new PopupMenu(binding.getRoot().getContext(), binding.buttonEntryMenu);
                popupMenu.inflate(R.menu.menu_weight_entry_actions);
                popupMenu.setOnMenuItemClickListener(item -> {
                    int itemId = item.getItemId();
                    if (itemId == R.id.menu_edit_entry) {
                        entryActionListener.onEdit(entry);
                        return true;
                    }
                    if (itemId == R.id.menu_delete_entry) {
                        entryActionListener.onDelete(entry);
                        return true;
                    }
                    return false;
                });
                popupMenu.show();
            });
        }
    }
}
