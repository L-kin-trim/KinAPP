package com.example.kinapp.fragment;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.Spinner;

import androidx.fragment.app.Fragment;

import com.example.kinapp.R;
import com.example.kinapp.activity.CreateZhanShuActivity;
import com.example.kinapp.activity.ZhanShuDetailActivity;
import com.example.kinapp.adapter.GroupZhanShuAdapter;
import com.example.kinapp.utils.ZhanShuDAO;
import com.example.kinapp.utils.ZhanShuInformationDAO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ZhanShuFragment extends Fragment {
    private Spinner mapSpinner;
    private Button addMapButton, deleteMapButton, addZhanShuButton, deleteZhanShuButton, selectAllButton, cancelSelectButton;
    private ExpandableListView expandableListView;
    private ZhanShuDAO zhanShuDAO;
    private ZhanShuInformationDAO zhanShuInformationDAO;
    private GroupZhanShuAdapter groupZhanShuAdapter;
    private boolean isSelectionMode = false;
    private List<ZhanShuDAO.MapItem> mapItems;
    private static final int REQUEST_CODE_CREATE_ZHANSHU = 1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_zhan_shu, container, false);
        mapSpinner = view.findViewById(R.id.map_spinner);
        addMapButton = view.findViewById(R.id.add_map_button);
        deleteMapButton = view.findViewById(R.id.delete_map_button);
        addZhanShuButton = view.findViewById(R.id.add_zhanshu_button);
        deleteZhanShuButton = view.findViewById(R.id.delete_zhanshu_button);
        selectAllButton = view.findViewById(R.id.select_all_button);
        cancelSelectButton = view.findViewById(R.id.cancel_select_button);
        expandableListView = view.findViewById(R.id.zhanshu_expandable_list);

        zhanShuDAO = new ZhanShuDAO(getActivity());
        zhanShuInformationDAO = new ZhanShuInformationDAO(getActivity());
        groupZhanShuAdapter = new GroupZhanShuAdapter(getActivity());
        groupZhanShuAdapter.setOnEditClickListener(new GroupZhanShuAdapter.OnEditClickListener() {
            @Override
            public void onEditClick(int id) {
                // 启动创建战术Activity进行编辑
                Intent intent = new Intent(getActivity(), CreateZhanShuActivity.class);
                intent.putExtra("zhanShuId", id);
                startActivityForResult(intent, REQUEST_CODE_CREATE_ZHANSHU);
            }
        });

        groupZhanShuAdapter.setOnViewClickListener(new GroupZhanShuAdapter.OnViewClickListener() {
            @Override
            public void onViewClick(int id) {
                // 启动战术详情Activity查看详情
                Intent intent = new Intent(getActivity(), ZhanShuDetailActivity.class);
                intent.putExtra("zhanshuId", id);
                startActivity(intent);
            }
        });
        expandableListView.setAdapter(groupZhanShuAdapter);

        loadMapData();
        setupListeners();

        return view;
    }

    private void loadMapData() {
        mapItems = zhanShuDAO.getAllMaps();
        List<String> mapNames = new ArrayList<>();
        for (ZhanShuDAO.MapItem map : mapItems) {
            mapNames.add(map.getName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, mapNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mapSpinner.setAdapter(adapter);

        if (!mapItems.isEmpty()) {
            loadZhanShuInformation(mapItems.get(0).getId());
        }
    }

    private void loadZhanShuInformation(int mapId) {
        List<GroupZhanShuAdapter.GroupItem> groupedItems = new ArrayList<>();
        Map<Integer, GroupZhanShuAdapter.GroupItem> typeToGroup = new HashMap<>();

        // 直接从zhanshu_information表中查询所有战术信息，按类型分组
        for (int type = 1; type <= 5; type++) {
            List<ZhanShuInformationDAO.ZhanShuInformationItem> infoItems = 
                zhanShuInformationDAO.getZhanShuInformationByMapIdAndType(mapId, type);
            
            if (!infoItems.isEmpty()) {
                GroupZhanShuAdapter.GroupItem group = new GroupZhanShuAdapter.GroupItem();
                group.type = type;
                group.typeName = getZhanShuTypeName(type);
                group.children = new ArrayList<>();
                
                // 转换ZhanShuInformationItem为ZhanShuItem
                for (ZhanShuInformationDAO.ZhanShuInformationItem infoItem : infoItems) {
                    group.children.add(new GroupZhanShuAdapter.ZhanShuItem(
                            infoItem.getId(), 
                            infoItem.getName(), 
                            infoItem.getDescription()
                    ));
                }
                
                typeToGroup.put(type, group);
                groupedItems.add(group);
            }
        }

        groupZhanShuAdapter.setData(groupedItems);
    }

    private String getZhanShuTypeName(int type) {
        switch (type) {
            case 1:
                return "Rush";
            case 2:
                return "Split";
            case 3:
                return "Contain";
            case 4:
                return "Lurk";
            case 5:
                return "Default";
            default:
                return "Other";
        }
    }

    private void setupListeners() {
        mapSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!mapItems.isEmpty()) {
                    loadZhanShuInformation(mapItems.get(position).getId());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        addMapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final EditText mapNameEditText = new EditText(getActivity());
                new AlertDialog.Builder(getActivity())
                        .setTitle("添加地图")
                        .setView(mapNameEditText)
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String mapName = mapNameEditText.getText().toString().trim();
                                if (!mapName.isEmpty()) {
                                    zhanShuDAO.insertMap(mapName);
                                    loadMapData();
                                }
                            }
                        })
                        .setNegativeButton("取消", null)
                        .show();
            }
        });

        deleteMapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mapItems.isEmpty()) {
                    final int selectedMapId = mapItems.get(mapSpinner.getSelectedItemPosition()).getId();
                    new AlertDialog.Builder(getActivity())
                            .setTitle("删除地图")
                            .setMessage("确定要删除当前地图吗？")
                            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    zhanShuDAO.deleteMap(selectedMapId);
                                    loadMapData();
                                }
                            })
                            .setNegativeButton("取消", null)
                            .show();
                }
            }
        });

        addZhanShuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mapItems.isEmpty()) {
                    int mapId = mapItems.get(mapSpinner.getSelectedItemPosition()).getId();
                    Intent intent = new Intent(getActivity(), CreateZhanShuActivity.class);
                    intent.putExtra("mapId", mapId);
                    startActivityForResult(intent, REQUEST_CODE_CREATE_ZHANSHU);
                }
            }
        });

        deleteZhanShuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isSelectionMode) {
                    isSelectionMode = true;
                    groupZhanShuAdapter.setShowCheckBoxes(true);
                    deleteZhanShuButton.setText("确认删除");
                    selectAllButton.setVisibility(View.VISIBLE);
                    cancelSelectButton.setVisibility(View.VISIBLE);
                } else {
                    if (groupZhanShuAdapter.getSelectedCount() > 0) {
                        new AlertDialog.Builder(getActivity())
                                .setTitle("删除战术")
                                .setMessage("确定要删除选中的战术吗？")
                                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        groupZhanShuAdapter.deleteSelectedItems(zhanShuInformationDAO);
                                        exitSelectionMode();
                                    }
                                })
                                .setNegativeButton("取消", null)
                                .show();
                    } else {
                        exitSelectionMode();
                    }
                }
            }
        });

        selectAllButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                groupZhanShuAdapter.selectAll(true);
            }
        });

        cancelSelectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exitSelectionMode();
            }
        });

        // 添加战术点击事件，跳转到战术详情页面
        expandableListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                if (!isSelectionMode) {
                    GroupZhanShuAdapter.ZhanShuItem item = groupZhanShuAdapter.getData().get(groupPosition).children.get(childPosition);
                    Intent intent = new Intent(getActivity(), ZhanShuDetailActivity.class);
                    intent.putExtra("name", item.name);
                    intent.putExtra("type", getZhanShuTypeName(groupZhanShuAdapter.getData().get(groupPosition).type));
                    intent.putExtra("description", item.description);
                    startActivity(intent);
                    return true;
                }
                return false;
            }
        });

        groupZhanShuAdapter.setOnItemSelectionChangedListener(new GroupZhanShuAdapter.OnItemSelectionChangedListener() {
            @Override
            public void onItemSelectionChanged() {
                int selectedCount = groupZhanShuAdapter.getSelectedCount();
                deleteZhanShuButton.setText("确认删除 (" + selectedCount + ")");
            }

            @Override
            public void onSelectionModeChanged(boolean isInSelectionMode) {
                ZhanShuFragment.this.isSelectionMode = isInSelectionMode;
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_CREATE_ZHANSHU) {
            // 刷新战术列表
            if (!mapItems.isEmpty()) {
                int mapId = mapItems.get(mapSpinner.getSelectedItemPosition()).getId();
                loadZhanShuInformation(mapId);
            }
        }
    }

    private void exitSelectionMode() {
        isSelectionMode = false;
        groupZhanShuAdapter.setShowCheckBoxes(false);
        deleteZhanShuButton.setText("删除战术");
        selectAllButton.setVisibility(View.GONE);
        cancelSelectButton.setVisibility(View.GONE);
    }
}
