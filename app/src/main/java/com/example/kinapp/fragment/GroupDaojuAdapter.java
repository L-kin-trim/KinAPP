package com.example.kinapp.fragment;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.kinapp.DaojuInformationActivity;
import com.example.kinapp.DaojuSingleActivity;
import com.example.kinapp.R;
import com.example.kinapp.utils.DaojuInformationDAO;
import com.example.kinapp.utils.MapDAO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupDaojuAdapter extends BaseAdapter {
    private static final int TYPE_GROUP = 0;
    private static final int TYPE_CHILD = 1;

    private List<GroupItem> groupedDaojuItems;
    private Context context;
    private DaojuInformationDAO daojuInformationDAO;
    private boolean showCheckBoxes = false;
    private OnItemSelectionChangedListener selectionListener;

    // 道具项内部类
    public static class DaojuItem {
        String text;
        boolean isSelected;
        int infoId; // 道具信息ID，用于删除操作

        DaojuItem(String text, boolean isSelected, int infoId) {
            this.text = text;
            this.isSelected = isSelected;
            this.infoId = infoId;
        }
    }

    // 分组项内部类
    public static class GroupItem {
        String groupName;
        List<DaojuItem> children;
        boolean isExpanded;

        GroupItem(String groupName) {
            this.groupName = groupName;
            this.children = new ArrayList<>();
            this.isExpanded = false;
        }
    }

    public GroupDaojuAdapter(Context context, DaojuInformationDAO daojuInformationDAO) {
        this.context = context;
        this.daojuInformationDAO = daojuInformationDAO;
        this.groupedDaojuItems = new ArrayList<>();
    }

    public void setOnItemSelectionChangedListener(OnItemSelectionChangedListener listener) {
        this.selectionListener = listener;
    }

    public void setShowCheckBoxes(boolean show) {
        this.showCheckBoxes = show;
        notifyDataSetChanged();
    }

    public boolean isShowCheckBoxes() {
        return showCheckBoxes;
    }

    public void updateData(List<GroupItem> data) {
        this.groupedDaojuItems.clear();
        this.groupedDaojuItems.addAll(data);
        notifyDataSetChanged();
    }

    public List<GroupItem> getData() {
        return groupedDaojuItems;
    }

    @Override
    public int getCount() {
        int count = 0;
        for (GroupItem group : groupedDaojuItems) {
            count++; // 组头
            if (group.isExpanded) {
                count += group.children.size(); // 子项
            }
        }
        return count;
    }

    @Override
    public int getViewTypeCount() {
        return 2; // 组头和子项两种类型
    }

    @Override
    public int getItemViewType(int position) {
        // 计算当前位置对应的项类型
        int currentPosition = 0;
        for (GroupItem group : groupedDaojuItems) {
            if (currentPosition == position) {
                return TYPE_GROUP; // 组头
            }
            currentPosition++;

            if (group.isExpanded) {
                if (position < currentPosition + group.children.size()) {
                    return TYPE_CHILD; // 子项
                }
                currentPosition += group.children.size();
            }
        }
        return TYPE_GROUP;
    }

    @Override
    public Object getItem(int position) {
        // 计算当前位置对应的项
        int currentPosition = 0;
        for (GroupItem group : groupedDaojuItems) {
            if (currentPosition == position) {
                return group; // 组头
            }
            currentPosition++;

            if (group.isExpanded) {
                if (position < currentPosition + group.children.size()) {
                    return group.children.get(position - currentPosition); // 子项
                }
                currentPosition += group.children.size();
            }
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        int type = getItemViewType(position);

        if (type == TYPE_GROUP) {
            // 处理组头
            GroupViewHolder groupHolder;
            if (convertView == null || !(convertView.getTag() instanceof GroupViewHolder)) {
                convertView = LayoutInflater.from(context).inflate(R.layout.item_daoju_group, parent, false);
                groupHolder = new GroupViewHolder();
                groupHolder.textView = convertView.findViewById(R.id.tv_group_name);
                groupHolder.indicator = convertView.findViewById(R.id.iv_indicator);
                convertView.setTag(groupHolder);
            } else {
                groupHolder = (GroupViewHolder) convertView.getTag();
            }

            // 获取组数据
            int groupIndex = getGroupIndex(position);
            GroupItem group = groupedDaojuItems.get(groupIndex);

            // 设置组名和子项数量
            groupHolder.textView.setText(group.groupName + " (" + group.children.size() + ")");

            // 设置展开/收起图标
            groupHolder.indicator.setImageResource(group.isExpanded ?
                    android.R.drawable.arrow_down_float : android.R.drawable.arrow_up_float);

            // 设置点击事件
            final int finalGroupIndex = groupIndex;
            convertView.setOnClickListener(v -> toggleGroup(finalGroupIndex));

            return convertView;
        } else {
            // 处理子项
            ChildViewHolder childHolder;
            if (convertView == null || !(convertView.getTag() instanceof ChildViewHolder)) {
                convertView = LayoutInflater.from(context).inflate(R.layout.item_daoju, parent, false);
                childHolder = new ChildViewHolder();
                childHolder.checkBox = convertView.findViewById(R.id.cb_daoju_select);
                childHolder.textView = convertView.findViewById(R.id.tv_daoju_text);
                childHolder.modifyButton = convertView.findViewById(R.id.btn_modify);
                convertView.setTag(childHolder);
            } else {
                childHolder = (ChildViewHolder) convertView.getTag();
            }

            // 获取子项数据
            ChildPosition childPos = getChildPosition(position);
            DaojuItem item = groupedDaojuItems.get(childPos.groupIndex).children.get(childPos.childIndex);

            childHolder.textView.setText(item.text);

            // 根据状态显示或隐藏复选框
            childHolder.checkBox.setVisibility(showCheckBoxes ? View.VISIBLE : View.GONE);
            if (showCheckBoxes) {
                // 移除之前的监听器避免重复触发
                childHolder.checkBox.setOnCheckedChangeListener(null);
                childHolder.checkBox.setChecked(item.isSelected);

                // 为复选框设置监听器
                childHolder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    item.isSelected = isChecked;
                    // 检查是否所有项都被选中以更新全选复选框
                    if (selectionListener != null) {
                        selectionListener.onItemSelectionChanged();
                    }
                });
            }

            // 设置修改按钮点击事件
            final int infoId = item.infoId;
            childHolder.modifyButton.setOnClickListener(v -> {
                // 修改为跳转到DaojuInformationActivity而不是DaojuSingleActivity
                Intent intent = new Intent(context, DaojuInformationActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("info_id", infoId);
                // 需要获取地图信息并传递
                // 获取该道具项对应的地图ID和位置信息
                MapDAO mapDAO = new MapDAO(context);
                List<MapDAO.DaojuItem> daojuItems = mapDAO.getAllDaojuItemsByInfoId(infoId);
                if (!daojuItems.isEmpty()) {
                    MapDAO.DaojuItem daojuItem = daojuItems.get(0); // 取第一个匹配项
                    intent.putExtra("map_id", daojuItem.getMapId());
                    // 获取地图名称
                    MapDAO.MapItem mapItem = mapDAO.getMapById(daojuItem.getMapId());
                    if (mapItem != null) {
                        intent.putExtra("map_name", mapItem.getName());
                    }
                }
                context.startActivity(intent);
            });




            // 为整个列表项设置点击监听器
            convertView.setOnClickListener(v -> {
                if (showCheckBoxes) {
                    // 如果处于选择模式，点击任意位置切换复选框状态
                    childHolder.checkBox.setChecked(!childHolder.checkBox.isChecked());
                } else {
                    // 如果不处于选择模式，直接打开详情页
                    Intent intent = new Intent(context, DaojuSingleActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("info_id", infoId);
                    context.startActivity(intent);
                }
            });

            // 长按进入选择模式
            convertView.setOnLongClickListener(v -> {
                if (!showCheckBoxes) {
                    showCheckBoxes = true;
                    notifyDataSetChanged();
                    if (selectionListener != null) {
                        selectionListener.onSelectionModeChanged(true);
                    }
                    // 初始点击设为选中状态
                    childHolder.checkBox.setChecked(!childHolder.checkBox.isChecked());
                    return true;
                }
                return false;
            });

            return convertView;
        }
    }

    // 获取指定位置属于第几个组
    private int getGroupIndex(int position) {
        int currentPosition = 0;
        for (int i = 0; i < groupedDaojuItems.size(); i++) {
            GroupItem group = groupedDaojuItems.get(i);
            if (currentPosition == position) {
                return i;
            }
            currentPosition++;

            if (group.isExpanded) {
                if (position < currentPosition + group.children.size()) {
                    return i;
                }
                currentPosition += group.children.size();
            }
        }
        return groupedDaojuItems.size() - 1;
    }

    // 获取子项在组内的位置
    private ChildPosition getChildPosition(int position) {
        int currentPosition = 0;
        for (int i = 0; i < groupedDaojuItems.size(); i++) {
            GroupItem group = groupedDaojuItems.get(i);
            currentPosition++;

            if (group.isExpanded) {
                if (position < currentPosition + group.children.size()) {
                    return new ChildPosition(i, position - currentPosition);
                }
                currentPosition += group.children.size();
            }
        }
        return new ChildPosition(0, 0);
    }

    // 展开/收起组
    private void toggleGroup(int groupIndex) {
        GroupItem group = groupedDaojuItems.get(groupIndex);
        group.isExpanded = !group.isExpanded;
        notifyDataSetChanged();
    }

    // 全选或取消全选
    public void selectAll(boolean select) {
        for (GroupItem group : groupedDaojuItems) {
            for (DaojuItem item : group.children) {
                item.isSelected = select;
            }
        }
        notifyDataSetChanged();
    }

    // 获取选中的道具项数量
    public int getSelectedCount() {
        int count = 0;
        for (GroupItem group : groupedDaojuItems) {
            for (DaojuItem item : group.children) {
                if (item.isSelected) {
                    count++;
                }
            }
        }
        return count;
    }

    // 删除选中的道具项
    public boolean deleteSelectedItems(DaojuInformationDAO daojuInformationDAO) {
        boolean hasDeleted = false;

        // 从后往前遍历组和子项，避免删除时索引变化的问题
        for (int i = groupedDaojuItems.size() - 1; i >= 0; i--) {
            GroupItem group = groupedDaojuItems.get(i);
            for (int j = group.children.size() - 1; j >= 0; j--) {
                DaojuItem item = group.children.get(j);
                if (item.isSelected) {
                    // 从数据库中删除道具信息
                    int result = daojuInformationDAO.deleteDaojuInformation(item.infoId);
                    if (result > 0) {
                        // 从列表中移除
                        group.children.remove(j);
                        hasDeleted = true;
                    }
                }
            }

            // 如果组中没有子项了，移除整个组
            if (group.children.isEmpty()) {
                groupedDaojuItems.remove(i);
            }
        }

        if (hasDeleted) {
            notifyDataSetChanged();
        }
        return hasDeleted;
    }

    private class GroupViewHolder {
        TextView textView;
        ImageView indicator;
    }

    private class ChildViewHolder {
        CheckBox checkBox;
        TextView textView;
        ImageView modifyButton;
    }

    private class ChildPosition {
        int groupIndex;
        int childIndex;

        ChildPosition(int groupIndex, int childIndex) {
            this.groupIndex = groupIndex;
            this.childIndex = childIndex;
        }
    }

    public interface OnItemSelectionChangedListener {
        void onItemSelectionChanged();
        void onSelectionModeChanged(boolean isInSelectionMode);
    }
}
