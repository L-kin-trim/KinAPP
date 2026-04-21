package com.example.kin.ui.admin;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.kin.R;
import com.example.kin.data.KinRepository;
import com.example.kin.model.AuditLogModel;
import com.example.kin.model.ForumCommentModel;
import com.example.kin.model.ForumPostModel;
import com.example.kin.model.MessageBoardEntryModel;
import com.example.kin.model.PageResult;
import com.example.kin.model.ReportModel;
import com.example.kin.model.ReviewTemplateModel;
import com.example.kin.net.ApiCallback;
import com.example.kin.net.ApiException;
import com.example.kin.ui.common.KinUi;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AdminCenterActivity extends AppCompatActivity {
    private KinRepository repository;
    private LinearLayout contentLayout;
    private LinearLayout postsLayout;
    private LinearLayout commentsLayout;
    private LinearLayout reportsLayout;
    private LinearLayout boardLayout;
    private LinearLayout templatesLayout;
    private LinearLayout logsLayout;
    private LinearLayout usersLayout;
    private ProgressBar progressBar;
    private TextView statusView;
    private final List<ForumPostModel> pendingPosts = new ArrayList<>();
    private final List<ReviewTemplateModel> templateCache = new ArrayList<>();

    private String postFilterStatus = "PENDING";
    private String postFilterType = "";
    private String postFilterMap = "";
    private String postFilterAuthor = "";
    private String postFilterFrom = "";
    private String postFilterTo = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = new KinRepository(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(getColor(KinUi.isNight(this) ? R.color.kin_dark_bg : R.color.kin_light_bg));

        MaterialToolbar toolbar = new MaterialToolbar(this);
        toolbar.setTitle("管理员中心");
        toolbar.setNavigationIcon(android.R.drawable.ic_menu_revert);
        toolbar.setNavigationOnClickListener(v -> finish());
        root.addView(toolbar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ScrollView scrollView = new ScrollView(this);
        contentLayout = KinUi.vertical(this);
        contentLayout.setPadding(KinUi.dp(this, 18), KinUi.dp(this, 12), KinUi.dp(this, 18), KinUi.dp(this, 24));
        progressBar = new ProgressBar(this);
        statusView = KinUi.muted(this, "", 13);
        statusView.setVisibility(View.GONE);
        contentLayout.addView(progressBar);
        contentLayout.addView(statusView);
        renderShell();
        scrollView.addView(contentLayout);
        root.addView(scrollView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
        refreshAll();
    }

    private void renderShell() {
        contentLayout.addView(sectionTitle("运营总览"));
        contentLayout.addView(buildOverviewCard());

        contentLayout.addView(sectionTitle("帖子审核"));
        contentLayout.addView(buildPostToolsCard());
        contentLayout.addView(buildPostFilterCard());
        postsLayout = KinUi.vertical(this);
        contentLayout.addView(postsLayout);

        contentLayout.addView(sectionTitle("评论审核"));
        commentsLayout = KinUi.vertical(this);
        contentLayout.addView(commentsLayout);

        contentLayout.addView(sectionTitle("举报处理"));
        reportsLayout = KinUi.vertical(this);
        contentLayout.addView(reportsLayout);

        contentLayout.addView(sectionTitle("留言板管理"));
        boardLayout = KinUi.vertical(this);
        contentLayout.addView(boardLayout);

        contentLayout.addView(sectionTitle("审核模板"));
        templatesLayout = KinUi.vertical(this);
        contentLayout.addView(templatesLayout);

        contentLayout.addView(sectionTitle("审计日志"));
        logsLayout = KinUi.vertical(this);
        contentLayout.addView(logsLayout);

        contentLayout.addView(sectionTitle("用户审计"));
        contentLayout.addView(buildUserSearchCard());
        usersLayout = KinUi.vertical(this);
        contentLayout.addView(usersLayout);
    }

    private View sectionTitle(String label) {
        TextView title = KinUi.text(this, label, 20, true);
        KinUi.margins(title, this, 0, 16, 0, 10);
        return title;
    }

    private View buildOverviewCard() {
        MaterialCardView card = KinUi.card(this);
        LinearLayout body = KinUi.sectionContainer(this, 18);
        MaterialButton refreshButton = KinUi.filledButton(this, "刷新总览");
        refreshButton.setOnClickListener(v -> loadOverview(body));
        body.addView(refreshButton);
        card.addView(body);
        loadOverview(body);
        return card;
    }

    private View buildPostToolsCard() {
        MaterialCardView card = KinUi.card(this);
        LinearLayout body = KinUi.sectionContainer(this, 18);
        MaterialButton batchApprove = KinUi.filledButton(this, "批量通过前 3 条");
        MaterialButton batchReject = KinUi.outlinedButton(this, "批量驳回前 3 条");
        batchApprove.setOnClickListener(v -> batchReview("APPROVED"));
        batchReject.setOnClickListener(v -> batchReview("REJECTED"));
        body.addView(batchApprove);
        body.addView(batchReject);
        KinUi.margins(batchReject, this, 0, 12, 0, 0);
        card.addView(body);
        return card;
    }

    private View buildPostFilterCard() {
        MaterialCardView card = KinUi.card(this);
        LinearLayout body = KinUi.sectionContainer(this, 18);
        body.addView(KinUi.text(this, "审核筛选", 16, true));
        TextInputLayout statusLayout = KinUi.inputLayout(this, "状态（默认 PENDING）", false);
        TextInputLayout typeLayout = KinUi.inputLayout(this, "类型（PROP_SHARE/TACTIC_SHARE...）", false);
        TextInputLayout mapLayout = KinUi.inputLayout(this, "地图关键词", false);
        TextInputLayout authorLayout = KinUi.inputLayout(this, "作者关键词", false);
        TextInputLayout fromLayout = KinUi.inputLayout(this, "开始时间（ISO）", false);
        TextInputLayout toLayout = KinUi.inputLayout(this, "结束时间（ISO）", false);
        TextInputEditText statusEdit = KinUi.edit(statusLayout);
        TextInputEditText typeEdit = KinUi.edit(typeLayout);
        TextInputEditText mapEdit = KinUi.edit(mapLayout);
        TextInputEditText authorEdit = KinUi.edit(authorLayout);
        TextInputEditText fromEdit = KinUi.edit(fromLayout);
        TextInputEditText toEdit = KinUi.edit(toLayout);
        statusEdit.setText(postFilterStatus);
        body.addView(statusLayout);
        body.addView(typeLayout);
        body.addView(mapLayout);
        body.addView(authorLayout);
        body.addView(fromLayout);
        body.addView(toLayout);
        KinUi.margins(typeLayout, this, 0, 10, 0, 0);
        KinUi.margins(mapLayout, this, 0, 10, 0, 0);
        KinUi.margins(authorLayout, this, 0, 10, 0, 0);
        KinUi.margins(fromLayout, this, 0, 10, 0, 0);
        KinUi.margins(toLayout, this, 0, 10, 0, 0);

        MaterialButton applyButton = KinUi.filledButton(this, "应用筛选");
        applyButton.setOnClickListener(v -> {
            postFilterStatus = TextUtils.isEmpty(text(statusEdit)) ? "PENDING" : text(statusEdit);
            postFilterType = text(typeEdit);
            postFilterMap = text(mapEdit);
            postFilterAuthor = text(authorEdit);
            postFilterFrom = text(fromEdit);
            postFilterTo = text(toEdit);
            loadPendingPosts();
        });
        KinUi.margins(applyButton, this, 0, 12, 0, 0);
        body.addView(applyButton);
        card.addView(body);
        return card;
    }

    private View buildUserSearchCard() {
        MaterialCardView card = KinUi.card(this);
        LinearLayout body = KinUi.sectionContainer(this, 18);
        TextInputLayout inputLayout = KinUi.inputLayout(this, "用户 ID 或用户名", false);
        TextInputEditText editText = KinUi.edit(inputLayout);
        MaterialButton searchButton = KinUi.filledButton(this, "查询用户");
        searchButton.setOnClickListener(v -> searchUsers(text(editText)));
        body.addView(inputLayout);
        KinUi.margins(searchButton, this, 0, 12, 0, 0);
        body.addView(searchButton);
        card.addView(body);
        return card;
    }

    private void refreshAll() {
        loadPendingPosts();
        loadPendingComments();
        loadReports();
        loadMessageBoard();
        loadTemplates();
        loadAuditLogs();
        searchUsers("");
    }

    private void loadOverview(LinearLayout target) {
        setLoading(true, "正在拉取管理数据…");
        repository.getAdminOverview(30, new ApiCallback<>() {
            @Override
            public void onSuccess(JSONObject data) {
                while (target.getChildCount() > 1) {
                    target.removeViewAt(1);
                }
                JSONObject summary = data.optJSONObject("summary");
                JSONObject moderation = data.optJSONObject("moderation");
                if (summary == null) {
                    summary = new JSONObject();
                }
                if (moderation == null) {
                    moderation = new JSONObject();
                }
                target.addView(info("总用户", String.valueOf(summary.optInt("totalUsers"))));
                target.addView(info("总帖子", String.valueOf(summary.optInt("totalPosts"))));
                target.addView(info("总评论", String.valueOf(summary.optInt("totalComments"))));
                target.addView(info("待审帖子", String.valueOf(moderation.optInt("pendingPostCount"))));
                target.addView(info("待审评论", String.valueOf(moderation.optInt("pendingCommentCount"))));
                setLoading(false, "");
            }

            @Override
            public void onError(ApiException exception) {
                setLoading(false, "总览加载失败：" + exception.getMessage());
            }
        });
    }

    private View info(String label, String value) {
        TextView textView = KinUi.muted(this, label + "：" + value, 14);
        KinUi.margins(textView, this, 0, 8, 0, 0);
        return textView;
    }

    private void loadPendingPosts() {
        repository.getAdminPosts(
                postFilterStatus,
                postFilterType,
                postFilterMap,
                postFilterAuthor,
                postFilterFrom,
                postFilterTo,
                0,
                10,
                new ApiCallback<>() {
            @Override
            public void onSuccess(PageResult<ForumPostModel> data) {
                pendingPosts.clear();
                pendingPosts.addAll(data.items);
                postsLayout.removeAllViews();
                for (ForumPostModel item : data.items) {
                    postsLayout.addView(postCard(item));
                }
            }

            @Override
            public void onError(ApiException exception) {
                setLoading(false, "帖子审核列表失败：" + exception.getMessage());
            }
        });
    }

    private void batchReview(String reviewStatus) {
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < Math.min(3, pendingPosts.size()); i++) {
            ids.add(pendingPosts.get(i).id);
        }
        if (ids.isEmpty()) {
            setLoading(false, "当前没有可批量处理的帖子。");
            return;
        }
        repository.batchReviewPosts(ids, reviewStatus, "管理员批量处理", 0L, new ApiCallback<>() {
            @Override
            public void onSuccess(JSONObject data) {
                loadPendingPosts();
            }

            @Override
            public void onError(ApiException exception) {
                setLoading(false, "批量审核失败：" + exception.getMessage());
            }
        });
    }

    private View postCard(ForumPostModel item) {
        MaterialCardView card = KinUi.card(this);
        LinearLayout body = KinUi.sectionContainer(this, 16);
        body.addView(KinUi.text(this, item.title, 17, true));
        body.addView(info("作者", item.createdByUsername));
        body.addView(info("类型", item.postType));
        TextView summary = KinUi.muted(this, TextUtils.isEmpty(item.content) ? item.tacticDescription : item.content, 14);
        KinUi.margins(summary, this, 0, 10, 0, 0);
        body.addView(summary);
        body.addView(actionRow(
                buildButton("通过", v -> reviewPost(item.id, "APPROVED", "管理员审核通过")),
                buildButton("驳回", v -> showRejectDialog(item.id)),
                buildButton("删除", v -> deletePost(item.id))
        ));
        card.addView(body);
        return card;
    }

    private void showRejectDialog(long postId) {
        LinearLayout root = KinUi.vertical(this);
        TextInputLayout remarkLayout = KinUi.inputLayout(this, "驳回备注（可留空，使用模板）", true);
        TextInputLayout templateLayout = KinUi.inputLayout(this, "模板ID（可选）", false);
        TextInputEditText remarkEdit = KinUi.edit(remarkLayout);
        TextInputEditText templateEdit = KinUi.edit(templateLayout);
        if (!templateCache.isEmpty()) {
            ReviewTemplateModel first = templateCache.get(0);
            templateEdit.setText(String.valueOf(first.id));
            StringBuilder helper = new StringBuilder();
            helper.append("模板：");
            for (ReviewTemplateModel model : templateCache) {
                helper.append(model.id).append("-").append(model.templateName).append("  ");
            }
            TextView tips = KinUi.muted(this, helper.toString(), 12);
            KinUi.margins(tips, this, 0, 8, 0, 0);
            root.addView(tips);
        }
        root.addView(remarkLayout);
        root.addView(templateLayout);
        KinUi.margins(remarkLayout, this, 0, 10, 0, 0);
        KinUi.margins(templateLayout, this, 0, 10, 0, 0);
        new AlertDialog.Builder(this)
                .setTitle("驳回帖子")
                .setView(root)
                .setPositiveButton("提交", (dialog, which) -> {
                    long templateId = 0L;
                    try {
                        templateId = Long.parseLong(text(templateEdit));
                    } catch (Exception ignored) {
                    }
                    repository.reviewPost(postId, "REJECTED", text(remarkEdit), templateId, new ApiCallback<>() {
                        @Override
                        public void onSuccess(ForumPostModel data) {
                            loadPendingPosts();
                        }

                        @Override
                        public void onError(ApiException exception) {
                            setLoading(false, "驳回失败：" + exception.getMessage());
                        }
                    });
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void reviewPost(long postId, String status, String remark) {
        repository.reviewPost(postId, status, remark, new ApiCallback<>() {
            @Override
            public void onSuccess(ForumPostModel data) {
                loadPendingPosts();
            }

            @Override
            public void onError(ApiException exception) {
                setLoading(false, "处理失败：" + exception.getMessage());
            }
        });
    }

    private void deletePost(long postId) {
        repository.deletePostAdmin(postId, "deleted by admin", new ApiCallback<>() {
            @Override
            public void onSuccess(ForumPostModel data) {
                loadPendingPosts();
            }

            @Override
            public void onError(ApiException exception) {
                setLoading(false, "删除失败：" + exception.getMessage());
            }
        });
    }

    private void loadPendingComments() {
        repository.getAdminComments("PENDING", 0, 10, new ApiCallback<>() {
            @Override
            public void onSuccess(PageResult<ForumCommentModel> data) {
                commentsLayout.removeAllViews();
                for (ForumCommentModel item : data.items) {
                    commentsLayout.addView(commentCard(item));
                }
            }

            @Override
            public void onError(ApiException exception) {
                setLoading(false, "评论列表失败：" + exception.getMessage());
            }
        });
    }

    private View commentCard(ForumCommentModel item) {
        MaterialCardView card = KinUi.card(this);
        LinearLayout body = KinUi.sectionContainer(this, 16);
        body.addView(KinUi.text(this, "#" + item.floorNumber + " · " + item.username, 16, true));
        TextView content = KinUi.muted(this, item.content, 14);
        KinUi.margins(content, this, 0, 8, 0, 0);
        body.addView(content);
        body.addView(actionRow(
                buildButton("通过", v -> reviewComment(item.id, "APPROVED")),
                buildButton("驳回", v -> reviewComment(item.id, "REJECTED")),
                buildButton("删除", v -> deleteComment(item.id))
        ));
        card.addView(body);
        return card;
    }

    private void reviewComment(long commentId, String status) {
        repository.reviewComment(commentId, status, "管理员处理", new ApiCallback<>() {
            @Override
            public void onSuccess(JSONObject data) {
                loadPendingComments();
            }

            @Override
            public void onError(ApiException exception) {
                setLoading(false, "评论处理失败：" + exception.getMessage());
            }
        });
    }

    private void deleteComment(long commentId) {
        repository.deleteCommentAdmin(commentId, new ApiCallback<>() {
            @Override
            public void onSuccess(JSONObject data) {
                loadPendingComments();
            }

            @Override
            public void onError(ApiException exception) {
                setLoading(false, "评论删除失败：" + exception.getMessage());
            }
        });
    }

    private void loadReports() {
        repository.getAdminReports("PENDING", "", "", "", 0, 10, new ApiCallback<>() {
            @Override
            public void onSuccess(PageResult<ReportModel> data) {
                reportsLayout.removeAllViews();
                for (ReportModel item : data.items) {
                    reportsLayout.addView(reportCard(item));
                }
            }

            @Override
            public void onError(ApiException exception) {
                reportsLayout.removeAllViews();
                reportsLayout.addView(info("状态", exception.isFeatureUnavailable() ? "举报接口未开放" : "举报加载失败"));
            }
        });
    }

    private View reportCard(ReportModel item) {
        MaterialCardView card = KinUi.card(this);
        LinearLayout body = KinUi.sectionContainer(this, 16);
        body.addView(KinUi.text(this, item.targetType + " #" + item.targetId, 16, true));
        body.addView(info("原因", item.reasonType));
        TextView detail = KinUi.muted(this, item.reasonDetail, 14);
        KinUi.margins(detail, this, 0, 8, 0, 0);
        body.addView(detail);
        MaterialButton processButton = KinUi.filledButton(this, "标记已处理");
        processButton.setOnClickListener(v -> repository.handleReport(item.id, "管理员已处理", new ApiCallback<>() {
            @Override
            public void onSuccess(ReportModel data) {
                loadReports();
            }

            @Override
            public void onError(ApiException exception) {
                setLoading(false, "举报处理失败：" + exception.getMessage());
            }
        }));
        KinUi.margins(processButton, this, 0, 12, 0, 0);
        body.addView(processButton);
        card.addView(body);
        return card;
    }

    private void loadMessageBoard() {
        repository.getAdminMessageBoardEntries("", "", 0, 10, new ApiCallback<>() {
            @Override
            public void onSuccess(PageResult<MessageBoardEntryModel> data) {
                boardLayout.removeAllViews();
                for (MessageBoardEntryModel item : data.items) {
                    boardLayout.addView(boardCard(item));
                }
            }

            @Override
            public void onError(ApiException exception) {
                boardLayout.removeAllViews();
                boardLayout.addView(info("状态", exception.isFeatureUnavailable() ? "留言板管理接口未开放" : "留言板管理加载失败"));
            }
        });
    }

    private View boardCard(MessageBoardEntryModel item) {
        MaterialCardView card = KinUi.card(this);
        LinearLayout body = KinUi.sectionContainer(this, 16);
        body.addView(KinUi.text(this, item.authorUsername, 16, true));
        TextView content = KinUi.muted(this, item.content, 14);
        KinUi.margins(content, this, 0, 8, 0, 0);
        body.addView(content);
        MaterialButton detailButton = KinUi.outlinedButton(this, "查看详情");
        detailButton.setOnClickListener(v -> repository.getAdminMessageBoardEntry(item.id, new ApiCallback<>() {
            @Override
            public void onSuccess(MessageBoardEntryModel data) {
                StringBuilder builder = new StringBuilder();
                builder.append("作者：").append(data.authorUsername).append('\n');
                builder.append("状态：").append(data.status).append('\n');
                builder.append("创建时间：").append(data.createdAt).append('\n');
                builder.append("更新时间：").append(data.updatedAt).append('\n');
                builder.append("状态备注：").append(TextUtils.isEmpty(data.statusNote) ? "无" : data.statusNote).append("\n\n");
                builder.append(data.content);
                showTextDialog("留言详情", builder.toString());
            }

            @Override
            public void onError(ApiException exception) {
                setLoading(false, "留言详情读取失败：" + exception.getMessage());
            }
        }));
        KinUi.margins(detailButton, this, 0, 10, 0, 0);
        body.addView(detailButton);
        body.addView(actionRow(
                buildButton("采纳", v -> updateBoard(item.id, "ADOPTED")),
                buildButton("已实现", v -> updateBoard(item.id, "IMPLEMENTED")),
                buildButton("撤回", v -> revokeBoard(item.id))
        ));
        card.addView(body);
        return card;
    }

    private void updateBoard(long entryId, String status) {
        repository.updateAdminMessageBoardStatus(entryId, status, "管理员更新状态", new ApiCallback<>() {
            @Override
            public void onSuccess(MessageBoardEntryModel data) {
                loadMessageBoard();
            }

            @Override
            public void onError(ApiException exception) {
                setLoading(false, "留言板状态更新失败：" + exception.getMessage());
            }
        });
    }

    private void revokeBoard(long entryId) {
        repository.revokeAdminMessageBoardEntry(entryId, "管理员撤回", new ApiCallback<>() {
            @Override
            public void onSuccess(MessageBoardEntryModel data) {
                loadMessageBoard();
            }

            @Override
            public void onError(ApiException exception) {
                setLoading(false, "留言撤回失败：" + exception.getMessage());
            }
        });
    }

    private void loadTemplates() {
        templatesLayout.removeAllViews();
        templateCache.clear();
        MaterialButton addButton = KinUi.filledButton(this, "新增模板");
        addButton.setOnClickListener(v -> showTemplateDialog(null));
        templatesLayout.addView(addButton);
        repository.getReviewTemplates(false, new ApiCallback<>() {
            @Override
            public void onSuccess(List<ReviewTemplateModel> data) {
                templateCache.addAll(data);
                for (ReviewTemplateModel item : data) {
                    templatesLayout.addView(templateCard(item));
                }
            }

            @Override
            public void onError(ApiException exception) {
                templatesLayout.addView(info("状态", exception.isFeatureUnavailable() ? "模板接口未开放" : "模板加载失败"));
            }
        });
    }

    private View templateCard(ReviewTemplateModel item) {
        MaterialCardView card = KinUi.card(this);
        LinearLayout body = KinUi.sectionContainer(this, 16);
        body.addView(KinUi.text(this, item.templateName, 16, true));
        TextView content = KinUi.muted(this, item.templateContent, 14);
        KinUi.margins(content, this, 0, 8, 0, 0);
        body.addView(content);
        body.addView(actionRow(
                buildButton("编辑", v -> showTemplateDialog(item)),
                buildButton("删除", v -> repository.deleteReviewTemplate(item.id, new ApiCallback<>() {
                    @Override
                    public void onSuccess(JSONObject data) {
                        loadTemplates();
                    }

                    @Override
                    public void onError(ApiException exception) {
                        setLoading(false, "模板删除失败：" + exception.getMessage());
                    }
                }))
        ));
        card.addView(body);
        return card;
    }

    private void showTemplateDialog(ReviewTemplateModel template) {
        LinearLayout root = KinUi.vertical(this);
        TextInputLayout nameLayout = KinUi.inputLayout(this, "模板名称", false);
        TextInputLayout contentLayout = KinUi.inputLayout(this, "模板内容", true);
        TextInputEditText nameEdit = KinUi.edit(nameLayout);
        TextInputEditText contentEdit = KinUi.edit(contentLayout);
        if (template != null) {
            nameEdit.setText(template.templateName);
            contentEdit.setText(template.templateContent);
        }
        root.addView(nameLayout);
        root.addView(contentLayout);
        KinUi.margins(contentLayout, this, 0, 12, 0, 0);
        new AlertDialog.Builder(this)
                .setTitle(template == null ? "新增模板" : "编辑模板")
                .setView(root)
                .setPositiveButton("保存", (dialog, which) -> {
                    ApiCallback<ReviewTemplateModel> callback = new ApiCallback<>() {
                        @Override
                        public void onSuccess(ReviewTemplateModel data) {
                            loadTemplates();
                        }

                        @Override
                        public void onError(ApiException exception) {
                            setLoading(false, "模板保存失败：" + exception.getMessage());
                        }
                    };
                    if (template == null) {
                        repository.createReviewTemplate(text(nameEdit), text(contentEdit), true, callback);
                    } else {
                        repository.updateReviewTemplate(template.id, text(nameEdit), text(contentEdit), true, callback);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void loadAuditLogs() {
        logsLayout.removeAllViews();
        MaterialButton exportButton = KinUi.outlinedButton(this, "导出 CSV");
        exportButton.setOnClickListener(v -> repository.exportAuditLogs("", "", "", "", new ApiCallback<>() {
            @Override
            public void onSuccess(String data) {
                showTextDialog("CSV 导出结果", data);
            }

            @Override
            public void onError(ApiException exception) {
                showTextDialog("CSV 导出结果", exception.isFeatureUnavailable() ? "后端未开放导出接口。" : "导出失败：" + exception.getMessage());
            }
        }));
        logsLayout.addView(exportButton);
        repository.getAuditLogs("", "", "", "", 0, 10, new ApiCallback<>() {
            @Override
            public void onSuccess(PageResult<AuditLogModel> data) {
                for (AuditLogModel item : data.items) {
                    logsLayout.addView(logCard(item));
                }
            }

            @Override
            public void onError(ApiException exception) {
                logsLayout.addView(info("状态", exception.isFeatureUnavailable() ? "审计日志接口未开放" : "审计日志加载失败"));
            }
        });
    }

    private View logCard(AuditLogModel item) {
        MaterialCardView card = KinUi.card(this);
        LinearLayout body = KinUi.sectionContainer(this, 16);
        body.addView(KinUi.text(this, item.actionType, 16, true));
        body.addView(info("管理员", item.adminUsername));
        body.addView(info("时间", item.createdAt));
        TextView detail = KinUi.muted(this, item.detail, 14);
        KinUi.margins(detail, this, 0, 8, 0, 0);
        body.addView(detail);
        card.addView(body);
        return card;
    }

    private void searchUsers(String keyword) {
        repository.searchUsers(keyword, 0, 10, new ApiCallback<>() {
            @Override
            public void onSuccess(JSONObject data) {
                usersLayout.removeAllViews();
                JSONArray items = data.optJSONArray("items");
                if (items == null) {
                    return;
                }
                for (int i = 0; i < items.length(); i++) {
                    JSONObject item = items.optJSONObject(i);
                    if (item != null) {
                        usersLayout.addView(userCard(item));
                    }
                }
            }

            @Override
            public void onError(ApiException exception) {
                usersLayout.removeAllViews();
                usersLayout.addView(info("状态", "用户搜索失败：" + exception.getMessage()));
            }
        });
    }

    private View userCard(JSONObject item) {
        MaterialCardView card = KinUi.card(this);
        LinearLayout body = KinUi.sectionContainer(this, 16);
        body.addView(KinUi.text(this, item.optString("username"), 17, true));
        body.addView(info("ID", String.valueOf(item.optLong("id"))));
        body.addView(info("角色", item.optString("role")));
        MaterialButton detail = KinUi.filledButton(this, "查看审计详情");
        detail.setOnClickListener(v -> openUserAudit(item.optLong("id")));
        KinUi.margins(detail, this, 0, 12, 0, 0);
        body.addView(detail);
        card.addView(body);
        return card;
    }

    private void openUserAudit(long userId) {
        repository.getUserAudit(userId, new ApiCallback<>() {
            @Override
            public void onSuccess(JSONObject data) {
                StringBuilder builder = new StringBuilder();
                builder.append("用户名：").append(data.optString("username")).append('\n');
                builder.append("邮箱：").append(data.optString("email")).append('\n');
                builder.append("角色：").append(data.optString("role")).append('\n');
                builder.append("发帖数：").append(data.optInt("postCount")).append('\n');
                builder.append("评论数：").append(data.optInt("commentCount")).append('\n');
                builder.append("自建库：").append(data.optInt("selfLibraryItemCount")).append('\n');
                builder.append("收藏数：").append(data.optInt("favoriteLibraryItemCount")).append('\n');
                showTextDialog("用户审计详情", builder.toString());
            }

            @Override
            public void onError(ApiException exception) {
                setLoading(false, "用户审计失败：" + exception.getMessage());
            }
        });
    }

    private LinearLayout actionRow(MaterialButton... buttons) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < buttons.length; i++) {
            row.addView(buttons[i]);
            if (i > 0) {
                KinUi.margins(buttons[i], this, 10, 0, 0, 0);
            }
        }
        KinUi.margins(row, this, 0, 12, 0, 0);
        return row;
    }

    private MaterialButton buildButton(String label, View.OnClickListener listener) {
        MaterialButton button = KinUi.outlinedButton(this, label);
        button.setOnClickListener(listener);
        return button;
    }

    private void showTextDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("关闭", null)
                .show();
    }

    private void setLoading(boolean loading, String message) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        statusView.setVisibility(TextUtils.isEmpty(message) ? View.GONE : View.VISIBLE);
        statusView.setText(message);
    }

    private String text(TextInputEditText editText) {
        return String.valueOf(editText.getText()).trim();
    }
}
