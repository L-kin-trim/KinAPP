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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.kinapp.DaojuInformationActivity;
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

    private String mParam1;
    private String mParam2;

    private ListView listMapName;
    private ListView listDaoju; // 道具列表
    private ArrayAdapter<String> mapNameAdapter;
    private GroupDaojuAdapter groupDaojuAdapter; // 分组道具适配器
    private List<String> mapNames;
    private MapDAO mapDAO;
    private DaojuInformationDAO daojuInformationDAO; // 道具信息DAO
    // ... existing code ...
    private int selectedPosition = -1; // 记录选中的位置
    private int selectedMapId = -1; // 记录选中的地图ID
    private CheckBox cbSelectAll; // 全选复选框
    private boolean shouldReloadData = false; // 标记是否需要重新加载数据

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

        // 默认隐藏全选复选框
        cbSelectAll.setVisibility(View.GONE);

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
        groupDaojuAdapter = new GroupDaojuAdapter(requireContext(), daojuInformationDAO);

        // 设置选择状态变化监听器
        groupDaojuAdapter.setOnItemSelectionChangedListener(new GroupDaojuAdapter.OnItemSelectionChangedListener() {
            @Override
            public void onItemSelectionChanged() {
                updateSelectAllState();
            }

            @Override
            public void onSelectionModeChanged(boolean isInSelectionMode) {
                // 显示或隐藏全选复选框
                cbSelectAll.setVisibility(isInSelectionMode ? View.VISIBLE : View.GONE);
                updateSelectAllState();
            }
        });

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

    // 在 DaoJuFragment 类中添加以下方法来启动 DaojuInformationActivity
    private void openDaojuInformationActivityForEdit(int daojuInfoId, String mapName) {
        Intent intent = new Intent(getActivity(), DaojuInformationActivity.class);
        intent.putExtra("info_id", daojuInfoId);
        intent.putExtra("map_name", mapName);
        startActivity(intent);

        // 标记需要在返回时重新加载数据
        shouldReloadData = true;
    }

    // 处理返回键事件
    public boolean onBackPressed() {
        // 如果处于选择模式，则退出选择模式
        if (groupDaojuAdapter.isShowCheckBoxes()) {
            exitSelectionMode();
            return true; // 已处理返回键事件
        }
        return false; // 未处理返回键事件
    }

    // 退出选择模式
    private void exitSelectionMode() {
        groupDaojuAdapter.setShowCheckBoxes(false);
        cbSelectAll.setVisibility(View.GONE);
        cbSelectAll.setChecked(false);
    }

    // 更新全选复选框的状态
    private void updateSelectAllState() {
        if (groupDaojuAdapter.getData().isEmpty()) {
            cbSelectAll.setChecked(false);
            return;
        }

        boolean allSelected = true;
        for (GroupDaojuAdapter.GroupItem group : groupDaojuAdapter.getData()) {
            for (GroupDaojuAdapter.DaojuItem item : group.children) {
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
        groupDaojuAdapter.selectAll(select);
    }

    // 确认删除选中的道具
    private void confirmDeleteDaoju() {
        // 统计选中的道具数量
        int selectedCount = groupDaojuAdapter.getSelectedCount();

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
        boolean hasDeleted = groupDaojuAdapter.deleteSelectedItems(daojuInformationDAO);

        if (hasDeleted) {
            Toast.makeText(getContext(), "道具删除成功", Toast.LENGTH_SHORT).show();

            // 清除全选状态
            cbSelectAll.setChecked(false);

            // 如果所有项目都被删除，退出选择模式
            if (groupDaojuAdapter.getCount() == 0) {
                groupDaojuAdapter.setShowCheckBoxes(false);
                cbSelectAll.setVisibility(View.GONE);
            }
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

        // 标记需要在返回时重新加载数据
        shouldReloadData = true;
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
        // 创建一个列表来存储分组数据
        List<GroupDaojuAdapter.GroupItem> groupedItems = new ArrayList<>();

        // 获取与该地图关联的所有道具
        List<MapDAO.DaojuItem> daojuList = mapDAO.getDaojuByMapId(mapId);

        // 创建一个Map来按位置分组
        Map<String, GroupDaojuAdapter.GroupItem> groups = new HashMap<>();

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
                GroupDaojuAdapter.GroupItem groupItem = groups.get(position);
                if (groupItem == null) {
                    groupItem = new GroupDaojuAdapter.GroupItem(position);
                    groups.put(position, groupItem);
                }

                // 添加道具项到对应的组
                GroupDaojuAdapter.DaojuItem item = new GroupDaojuAdapter.DaojuItem(itemText, false, info.getId());
                groupItem.children.add(item);
            }
        }

        // 将分组数据添加到列表中
        for (Map.Entry<String, GroupDaojuAdapter.GroupItem> entry : groups.entrySet()) {
            groupedItems.add(entry.getValue());
        }

        // 更新适配器数据
        groupDaojuAdapter.updateData(groupedItems);

        // 重置选择模式
        groupDaojuAdapter.setShowCheckBoxes(false);
        cbSelectAll.setVisibility(View.GONE);
        cbSelectAll.setChecked(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        // 当Fragment重新可见时，检查是否需要重新加载数据
        if (shouldReloadData && selectedMapId != -1) {
            loadDaojuInformation(selectedMapId);
            shouldReloadData = false;
        }
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
}
