package com.example.kinapp.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.example.kinapp.R;
import com.example.kinapp.utils.ZhanShuInformationDAO;

import java.util.ArrayList;
import java.util.List;

public class GroupZhanShuAdapter_temp extends BaseExpandableListAdapter {
    private Context context;
    private List<GroupItem> data;
    private boolean showCheckBoxes = false;
    private OnItemSelectionChangedListener selectionListener;

    public static class ZhanShuItem {
        public String name;
        public String description;
        public boolean isSelected;
        public int id;

        public ZhanShuItem(int id, String name, String description) {
            this.name = name;
            this.description = description;
            this.isSelected = false;
            this.id = id;
        }
    }

    public static class GroupItem {
        public String typeName;
        public List<ZhanShuItem> children;
        public int type;

        public GroupItem() {
            this.children = new ArrayList<>();
        }
    }

    public GroupZhanShuAdapter_temp(Context context) {
        this.context = context;
        this.data = new ArrayList<>();
    }

    public void setData(List<GroupItem> data) {
        this.data = data;
        notifyDataSetChanged();
    }

    public List<GroupItem> getData() {
        return data;
    }

    public void setShowCheckBoxes(boolean show) {
        showCheckBoxes = show;
        notifyDataSetChanged();
    }

    public boolean isShowCheckBoxes() {
        return showCheckBoxes;
    }

    public int getSelectedCount() {
        int count = 0;
        for (GroupItem group : data) {
            for (ZhanShuItem item : group.children) {
                if (item.isSelected) {
                    count++;
                }
            }
        }
        return count;
    }

    public boolean deleteSelectedItems(ZhanShuInformationDAO zhanShuInformationDAO) {
        boolean hasDeleted = false;
        List<GroupItem> groupsToRemove = new ArrayList<>();

        for (GroupItem group : data) {
            List<ZhanShuItem> itemsToRemove = new ArrayList<>();
            for (ZhanShuItem item : group.children) {
                if (item.isSelected) {
                    zhanShuInformationDAO.deleteZhanShuInformation(item.id);
                    itemsToRemove.add(item);
                    hasDeleted = true;
                }
            }
            group.children.removeAll(itemsToRemove);
            if (group.children.isEmpty()) {
                groupsToRemove.add(group);
            }
        }
        data.removeAll(groupsToRemove);
        notifyDataSetChanged();
        return hasDeleted;
    }

    public void selectAll(boolean select) {
        for (GroupItem group : data) {
            for (ZhanShuItem item : group.children) {
                item.isSelected = select;
            }
        }
        notifyDataSetChanged();
        if (selectionListener != null) {
            selectionListener.onItemSelectionChanged();
        }
    }

    public void setOnItemSelectionChangedListener(OnItemSelectionChangedListener listener) {
        this.selectionListener = listener;
    }

    public interface OnItemSelectionChangedListener {
        void onItemSelectionChanged();
        void onSelectionModeChanged(boolean isInSelectionMode);
    }

    @Override
    public int getGroupCount() {
        return data.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return data.get(groupPosition).children.size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return data.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return data.get(groupPosition).children.get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        GroupViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.group_item, parent, false);
            holder = new GroupViewHolder();
            holder.groupName = convertView.findViewById(R.id.group_name);
            holder.groupIndicator = convertView.findViewById(R.id.group_indicator);
            convertView.setTag(holder);
        } else {
            holder = (GroupViewHolder) convertView.getTag();
        }

        GroupItem group = data.get(groupPosition);
        holder.groupName.setText(group.typeName);
        holder.groupIndicator.setText(isExpanded ? "▲" : "▼");

        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        ChildViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.zhanshu_item, parent, false);
            holder = new ChildViewHolder();
            holder.checkBox = convertView.findViewById(R.id.checkbox);
            holder.zhanshuName = convertView.findViewById(R.id.zhanshu_name);
            holder.zhanshuDescription = convertView.findViewById(R.id.zhanshu_description);
            holder.editButton = convertView.findViewById(R.id.edit_button);
            convertView.setTag(holder);
        } else {
            holder = (ChildViewHolder) convertView.getTag();
        }

        GroupItem group = data.get(groupPosition);
        ZhanShuItem item = group.children.get(childPosition);

        holder.zhanshuName.setText(item.name);
        holder.zhanshuDescription.setText(item.description);
        holder.checkBox.setVisibility(showCheckBoxes ? View.VISIBLE : View.GONE);
        holder.checkBox.setChecked(item.isSelected);

        holder.checkBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                item.isSelected = holder.checkBox.isChecked();
                if (selectionListener != null) {
                    selectionListener.onItemSelectionChanged();
                }
            }
        });

        holder.editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onEditClickListener != null) {
                    onEditClickListener.onEditClick(item.id);
                }
            }
        });

        return convertView;
    }

    private OnEditClickListener onEditClickListener;

    public interface OnEditClickListener {
        void onEditClick(int id);
    }

    public void setOnEditClickListener(OnEditClickListener listener) {
        this.onEditClickListener = listener;
    }

    private static class GroupViewHolder {
        TextView groupName;
        TextView groupIndicator;
    }

    private static class ChildViewHolder {
        CheckBox checkBox;
        TextView zhanshuName;
        TextView zhanshuDescription;
        View editButton;
    }
}
