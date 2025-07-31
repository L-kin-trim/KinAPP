package com.example.kinapp.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.kinapp.DaojuInformationActivity;
import com.example.kinapp.DaojuSingleActivity;
import com.example.kinapp.R;
import com.example.kinapp.utils.MapDAO;
import com.example.kinapp.utils.MapDAO.MapItem;
import com.example.kinapp.utils.DaojuInformationDAO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DaoJuFragment extends Fragment {
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private ListView listMapName;
    private ListView listDaoju; // 道具列表
    private ArrayAdapter<String> mapNameAdapter;
    private GroupDaojuAdapter groupDaojuAdapter; // 分组道具适配器
    private List<String> mapNames;
    private List<GroupItem> groupedDaojuItems; // 分组的道具项列表
    private MapDAO mapDAO;
    private DaojuInformationDAO daojuInformationDAO; // 道具信息DAO
    private int selectedPosition = -1; // 记录选中的位置
    private int selectedMapId = -1; // 记录选中的地图ID
    private CheckBox cbSelectAll; // 全选复选框

    // 道具项内部类
    private static class DaojuItem {
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
    private static class GroupItem {
        String groupName;
        List<DaojuItem> children;
        boolean isExpanded;

        GroupItem(String groupName) {
            this.groupName = groupName;
            this.children = new ArrayList<>();
            this.isExpanded = false;
        }
    }

    public DaoJuFragment() {
        // Required empty public constructor
    }
    public static DaoJuFragment newInstance(String param1, String param2) {
        DaoJuFragment fragment = new DaoJuFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

        // 初始化数据
        mapNames = new ArrayList<>();
        groupedDaojuItems = new ArrayList<>(); // 初始化分组道具项列表
        Context context = getContext();
        if (context != null) {
            mapDAO = new MapDAO(context);
            daojuInformationDAO = new DaojuInformationDAO(context); // 初始化道具信息DAO
            loadMapNames();
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_dao_ju, container, false);

        // 初始化列表视图
        listMapName = view.findViewById(R.id.list_mapname);
        listDaoju = view.findViewById(R.id.list_daoju); // 初始化道具列表
        cbSelectAll = view.findViewById(R.id.cb_select_all); // 初始化全选复选框

        mapNameAdapter = new ArrayAdapter<String>(requireContext(), android.R.layout.simple_list_item_1, mapNames) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);

                // 如果是选中的项，改变背景色
                if (position == selectedPosition) {
                    view.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
                } else {
                    view.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                }

                return view;
            }
        };

        // 创建分组道具列表适配器
        groupDaojuAdapter = new GroupDaojuAdapter();

        listMapName.setAdapter(mapNameAdapter);
        listDaoju.setAdapter(groupDaojuAdapter); // 设置道具列表适配器

        // 设置列表项点击事件
        listMapName.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectedPosition = position;
                mapNameAdapter.notifyDataSetChanged(); // 刷新列表以显示选中状态

                // 获取选中的地图ID并加载相关道具信息
                String selectedMapName = mapNames.get(position);
                List<MapItem> maps = mapDAO.getAllMaps();
                for (MapItem map : maps) {
                    if (map.getName().equals(selectedMapName)) {
                        selectedMapId = map.getId();
                        loadDaojuInformation(selectedMapId); // 加载该地图的道具信息
                        break;
                    }
                }
            }
        });

        // 设置全选复选框的点击事件
        cbSelectAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isChecked = cbSelectAll.isChecked();
                selectAll(isChecked);
            }
        });

        // 设置添加和删除按钮的点击事件
        View addMapButton = view.findViewById(R.id.add_map);
        View minusMapButton = view.findViewById(R.id.minus_map);
        View minusDaojuButton = view.findViewById(R.id.minus_daoju); // 删除道具按钮
        View addDaojuButton = view.findViewById(R.id.add_daoju); // 添加道具按钮

        addMapButton.setOnClickListener(v -> showAddMapDialog());
        minusMapButton.setOnClickListener(v -> confirmDeleteMap());
        minusDaojuButton.setOnClickListener(v -> confirmDeleteDaoju()); // 删除选中的道具
        addDaojuButton.setOnClickListener(v -> openDaojuInformationActivity()); // 打开道具信息录入页面

        return view;
    }


    // 更新全选复选框的状态
    private void updateSelectAllState() {
        if (groupedDaojuItems.isEmpty()) {
            cbSelectAll.setChecked(false);
            return;
        }

        boolean allSelected = true;
        for (GroupItem group : groupedDaojuItems) {
            for (DaojuItem item : group.children) {
                if (!item.isSelected) {
                    allSelected = false;
                    break;
                }
            }
            if (!allSelected) {
                break;
            }
        }
        cbSelectAll.setChecked(allSelected);
    }

    // 全选或取消全选
    private void selectAll(boolean select) {
        for (GroupItem group : groupedDaojuItems) {
            for (DaojuItem item : group.children) {
                item.isSelected = select;
            }
        }
        groupDaojuAdapter.notifyDataSetChanged();
    }

    // 确认删除选中的道具
    private void confirmDeleteDaoju() {
        // 统计选中的道具数量
        int selectedCount = 0;
        for (GroupItem group : groupedDaojuItems) {
            for (DaojuItem item : group.children) {
                if (item.isSelected) {
                    selectedCount++;
                }
            }
        }

        if (selectedCount == 0) {
            Toast.makeText(getContext(), "请先选择要删除的道具", Toast.LENGTH_SHORT).show();
            return;
        }

        // 显示确认对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("确认删除");
        builder.setMessage("确定要删除选中的 " + selectedCount + " 个道具吗？此操作不可撤销");

        // 设置确认按钮
        builder.setPositiveButton("确定", (dialog, which) -> {
            deleteSelectedDaoju();
        });

        // 设置取消按钮
        builder.setNegativeButton("取消", (dialog, which) -> dialog.dismiss());

        // 显示对话框
        builder.show();
    }

    // 删除选中的道具
    private void deleteSelectedDaoju() {
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
            groupDaojuAdapter.notifyDataSetChanged();
            Toast.makeText(getContext(), "道具删除成功", Toast.LENGTH_SHORT).show();

            // 清除全选状态
            cbSelectAll.setChecked(false);
        } else {
            Toast.makeText(getContext(), "道具删除失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 显示添加地图对话框
     */
    private void showAddMapDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("添加地图");

        // 创建EditText用于输入地图名称
        final EditText input = new EditText(requireContext());
        input.setHint("请输入地图名称");
        builder.setView(input);

        // 设置确认和取消按钮
        builder.setPositiveButton("确认", (dialog, which) -> {
            String mapName = input.getText().toString().trim();
            if (!mapName.isEmpty()) {
                // 添加地图到数据库
                long result = mapDAO.insertMap(mapName, ""); // iconPosition暂时为空
                if (result != -1) {
                    Toast.makeText(requireContext(), "地图添加成功", Toast.LENGTH_SHORT).show();
                    loadMapNames(); // 重新加载地图列表
                } else {
                    Toast.makeText(requireContext(), "地图添加失败", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(requireContext(), "地图名称不能为空", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("取消", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    /**
     * 确认删除地图对话框
     */
    private void confirmDeleteMap() {
        if (selectedPosition == -1) {
            Toast.makeText(requireContext(), "请先选择要删除的地图", Toast.LENGTH_SHORT).show();
            return;
        }

        // 显示确认对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("确认删除");
        builder.setMessage("确定要删除地图 \"" + mapNames.get(selectedPosition) + "\" 吗？此操作不可撤销，同时也将删除您保存的所有道具信息");

        // 设置确认按钮
        builder.setPositiveButton("确定", (dialog, which) -> {
            // 获取选中的地图名称
            String selectedMapName = mapNames.get(selectedPosition);

            // 从数据库中查找该地图的ID
            List<MapItem> maps = mapDAO.getAllMaps();
            int mapId = -1;
            for (MapItem map : maps) {
                if (map.getName().equals(selectedMapName)) {
                    mapId = map.getId();
                    break;
                }
            }

            if (mapId != -1) {
                // 从数据库中删除地图
                int result = mapDAO.deleteMap(mapId);
                if (result > 0) {
                    Toast.makeText(requireContext(), "地图删除成功", Toast.LENGTH_SHORT).show();
                    loadMapNames(); // 重新加载地图列表

                    // 如果删除后列表不为空，保持选中第一个
                    if (!mapNames.isEmpty()) {
                        selectedPosition = 0;
                    } else {
                        selectedPosition = -1;
                    }

                    // 刷新界面
                    if (mapNameAdapter != null) {
                        mapNameAdapter.notifyDataSetChanged();
                    }
                } else {
                    Toast.makeText(requireContext(), "地图删除失败", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(requireContext(), "未找到要删除的地图", Toast.LENGTH_SHORT).show();
            }
        });

        // 设置取消按钮
        builder.setNegativeButton("取消", (dialog, which) -> dialog.dismiss());

        // 显示对话框
        builder.show();
    }


    /**
     * 打开道具信息录入页面
     */
    private void openDaojuInformationActivity() {
        if (selectedMapId == -1) {
            Toast.makeText(getContext(), "请先选择一个地图", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(getActivity(), DaojuInformationActivity.class);
        intent.putExtra("map_id", selectedMapId);
        String selectedMapName = mapNames.get(selectedPosition);
        intent.putExtra("map_name", selectedMapName);
        startActivity(intent);
    }


    /**
     * 加载地图名称列表
     */
    private void loadMapNames() {
        mapNames.clear();
        List<MapItem> maps = mapDAO.getAllMaps();
        for (MapItem map : maps) {
            mapNames.add(map.getName());
        }
        if (mapNameAdapter != null) {
            mapNameAdapter.notifyDataSetChanged();
        }
    }

    // 添加道具类型常量
    private static final int TYPE_SMOKE = 1;      // 烟雾弹
    private static final int TYPE_MOLOTOV = 2;    // 燃烧瓶
    private static final int TYPE_GRENADE = 3;    // 手雷
    private static final int TYPE_FLASHBANG = 4;  // 闪光弹

    /**
     * 根据地图ID加载相关道具信息
     * @param mapId 地图ID
     */
    private void loadDaojuInformation(int mapId) {
        // 清空分组数据
        groupedDaojuItems.clear();

        // 获取与该地图关联的所有道具
        List<MapDAO.DaojuItem> daojuList = mapDAO.getDaojuByMapId(mapId);

        // 创建一个Map来按位置分组
        Map<String, GroupItem> groups = new HashMap<>();

        // 获取每个道具的详细信息
        for (MapDAO.DaojuItem daojuItem : daojuList) {
            DaojuInformationDAO.DaojuInformation info = daojuInformationDAO.getDaojuInformationById(daojuItem.getInfoId());
            if (info != null) {
                String typeName = getDaojuTypeName(daojuItem.getType());
                String itemText = info.getToolName() + " - 类型: " + typeName;

                // 获取位置信息，如果没有指定则使用默认值
                String position = daojuItem.getPosition();
                if (position == null || position.isEmpty()) {
                    position = "未指定位置";
                }

                // 查找或创建组
                GroupItem groupItem = groups.get(position);
                if (groupItem == null) {
                    groupItem = new GroupItem(position);
                    groups.put(position, groupItem);
                }

                // 添加道具项到对应的组
                DaojuItem item = new DaojuItem(itemText, false, info.getId());
                groupItem.children.add(item);
            }
        }

        // 将分组数据添加到列表中
        for (Map.Entry<String, GroupItem> entry : groups.entrySet()) {
            groupedDaojuItems.add(entry.getValue());
        }

        if (groupDaojuAdapter != null) {
            groupDaojuAdapter.notifyDataSetChanged();
        }

        // 清除全选状态
        cbSelectAll.setChecked(false);
    }

    /**
     * 根据类型ID获取道具类型名称
     * @param typeId 类型ID
     * @return 类型名称
     */
    private String getDaojuTypeName(int typeId) {
        switch (typeId) {
            case TYPE_SMOKE:
                return "烟雾弹";
            case TYPE_MOLOTOV:
                return "燃烧瓶";
            case TYPE_GRENADE:
                return "手雷";
            case TYPE_FLASHBANG:
                return "闪光弹";
            default:
                return "未知";
        }
    }

    // 自定义分组道具列表适配器
    private class GroupDaojuAdapter extends BaseAdapter {
        private static final int TYPE_GROUP = 0;
        private static final int TYPE_CHILD = 1;

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
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_daoju_group, parent, false);
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
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_daoju, parent, false);
                    childHolder = new ChildViewHolder();
                    childHolder.checkBox = convertView.findViewById(R.id.cb_daoju_select);
                    childHolder.textView = convertView.findViewById(R.id.tv_daoju_text);
                    convertView.setTag(childHolder);
                } else {
                    childHolder = (ChildViewHolder) convertView.getTag();
                }

                // 获取子项数据
                ChildPosition childPos = getChildPosition(position);
                DaojuItem item = groupedDaojuItems.get(childPos.groupIndex).children.get(childPos.childIndex);

                childHolder.textView.setText(item.text);

                // 移除之前的监听器避免重复触发
                childHolder.checkBox.setOnCheckedChangeListener(null);
                childHolder.checkBox.setChecked(item.isSelected);

                // 为复选框设置监听器
                childHolder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    item.isSelected = isChecked;
                    // 检查是否所有项都被选中以更新全选复选框
                    updateSelectAllState();
                });

                // 为整个列表项设置点击监听器
                final int infoId = item.infoId;
                convertView.setOnClickListener(v -> {
                    Intent intent = new Intent(getActivity(), DaojuSingleActivity.class);
                    intent.putExtra("info_id", infoId);
                    startActivity(intent);
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

        private class GroupViewHolder {
            TextView textView;
            ImageView indicator;
        }

        private class ChildViewHolder {
            CheckBox checkBox;
            TextView textView;
        }

        private class ChildPosition {
            int groupIndex;
            int childIndex;

            ChildPosition(int groupIndex, int childIndex) {
                this.groupIndex = groupIndex;
                this.childIndex = childIndex;
            }
        }
    }
}
