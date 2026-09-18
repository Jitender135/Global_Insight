package com.example.global_insights;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.global_insights.Network.GrokStoryService;
import com.example.global_insights.model.Article;
import com.example.global_insights.model.AskStoryModel;
import com.example.global_insights.model.Story;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

public class AskStoryBottomSheetDialog extends BottomSheetDialogFragment {

    private Story story;
    private List<AskStoryModel.ChatMessage> chatList;
    private ChatAdapter chatAdapter;

    private TextView tvStoryTitle;
    private LinearLayout containerSuggestedChips;
    private RecyclerView rvChatMessages;
    private LinearLayout layoutThinking;
    private EditText etQuestionInput;
    private ImageView btnSendQuestion;
    private ImageView btnCloseSheet;

    public static AskStoryBottomSheetDialog newInstance(Article article) {
        AskStoryBottomSheetDialog dialog = new AskStoryBottomSheetDialog();
        dialog.story = Story.fromArticle(article);
        return dialog;
    }

    public static AskStoryBottomSheetDialog newInstance(Story story) {
        AskStoryBottomSheetDialog dialog = new AskStoryBottomSheetDialog();
        dialog.story = story;
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_ask_story, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvStoryTitle = view.findViewById(R.id.tv_sheet_story_title);
        containerSuggestedChips = view.findViewById(R.id.container_suggested_chips);
        rvChatMessages = view.findViewById(R.id.rv_chat_messages);
        layoutThinking = view.findViewById(R.id.layout_thinking);
        etQuestionInput = view.findViewById(R.id.et_question_input);
        btnSendQuestion = view.findViewById(R.id.btn_send_question);
        btnCloseSheet = view.findViewById(R.id.btn_close_sheet);

        if (story != null) {
            tvStoryTitle.setText(story.getTitle());
        }

        chatList = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatList);
        rvChatMessages.setLayoutManager(new LinearLayoutManager(getContext()));
        rvChatMessages.setAdapter(chatAdapter);

        btnCloseSheet.setOnClickListener(v -> dismiss());

        btnSendQuestion.setOnClickListener(v -> {
            String q = etQuestionInput.getText().toString().trim();
            if (!TextUtils.isEmpty(q)) {
                sendQuestion(q);
                etQuestionInput.setText("");
            }
        });

        loadSuggestedQuestions();

        // Welcome message
        AskStoryModel.ChatMessage welcome = new AskStoryModel.ChatMessage(
                AskStoryModel.ChatMessage.ROLE_AI,
                "<b>Ask me anything about this story!</b><br/>Tap a question chip below or ask your own question for instant facts."
        );
        chatList.add(welcome);
        chatAdapter.notifyItemInserted(chatList.size() - 1);
    }

    private void loadSuggestedQuestions() {
        if (story == null) return;
        GrokStoryService.generateSuggestedQuestions(story, new GrokStoryService.SuggestedQuestionsCallback() {
            @Override
            public void onSuccess(List<String> questions) {
                renderQuestionChips(questions);
            }

            @Override
            public void onError(String errorMessage) {
                // Handled via fallbacks
            }
        });
    }

    private void renderQuestionChips(List<String> questions) {
        if (getContext() == null || containerSuggestedChips == null || questions == null) return;
        containerSuggestedChips.removeAllViews();
        for (String q : questions) {
            TextView chip = new TextView(getContext());
            chip.setText(q);
            chip.setTextSize(12);
            chip.setPadding(28, 14, 28, 14);
            chip.setTextColor(android.graphics.Color.parseColor("#1D4ED8"));
            chip.setTypeface(null, android.graphics.Typeface.BOLD);
            chip.setBackgroundResource(R.drawable.bg_citation_badge);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 16, 0);
            chip.setLayoutParams(params);

            chip.setOnClickListener(v -> sendQuestion(q));
            containerSuggestedChips.addView(chip);
        }
    }

    private void sendQuestion(String userQuery) {
        if (story == null) return;

        // Add User Message
        AskStoryModel.ChatMessage userMsg = new AskStoryModel.ChatMessage(AskStoryModel.ChatMessage.ROLE_USER, userQuery);
        chatList.add(userMsg);
        chatAdapter.notifyItemInserted(chatList.size() - 1);
        rvChatMessages.smoothScrollToPosition(chatList.size() - 1);

        // Show thinking indicator
        layoutThinking.setVisibility(View.VISIBLE);

        GrokStoryService.askStoryQuestion(story, userQuery, chatList, new GrokStoryService.AskStoryCallback() {
            @Override
            public void onSuccess(String answerText, List<AskStoryModel.Citation> citations, List<String> followUps) {
                if (getContext() == null) return;
                layoutThinking.setVisibility(View.GONE);

                AskStoryModel.ChatMessage aiMsg = new AskStoryModel.ChatMessage(AskStoryModel.ChatMessage.ROLE_AI, answerText);
                aiMsg.setCitations(citations);
                chatList.add(aiMsg);
                chatAdapter.notifyItemInserted(chatList.size() - 1);
                rvChatMessages.smoothScrollToPosition(chatList.size() - 1);

                // Update follow-up question chips below for next user action!
                if (followUps != null && !followUps.isEmpty()) {
                    renderQuestionChips(followUps);
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (getContext() == null) return;
                layoutThinking.setVisibility(View.GONE);

                AskStoryModel.ChatMessage errorMsg = new AskStoryModel.ChatMessage(AskStoryModel.ChatMessage.ROLE_AI, errorMessage);
                chatList.add(errorMsg);
                chatAdapter.notifyItemInserted(chatList.size() - 1);
            }
        });
    }

    // Chat RecyclerView Adapter
    private class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
        private List<AskStoryModel.ChatMessage> messages;

        public ChatAdapter(List<AskStoryModel.ChatMessage> messages) {
            this.messages = messages;
        }

        @NonNull
        @Override
        public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false);
            return new ChatViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
            AskStoryModel.ChatMessage msg = messages.get(position);
            if (msg.getRole() == AskStoryModel.ChatMessage.ROLE_USER) {
                holder.layoutUser.setVisibility(View.VISIBLE);
                holder.layoutAi.setVisibility(View.GONE);
                holder.tvUserMsg.setText(msg.getText());
            } else {
                holder.layoutUser.setVisibility(View.GONE);
                holder.layoutAi.setVisibility(View.VISIBLE);

                String rawText = msg.getText();
                if (rawText != null) {
                    // Replace markdown bold with HTML bold and linebreaks with <br/>
                    rawText = rawText.replaceAll("\\*\\*(.*?)\\*\\*", "<b>$1</b>");
                    rawText = rawText.replace("\n", "<br/>");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        holder.tvAiMsg.setText(Html.fromHtml(rawText, Html.FROM_HTML_MODE_LEGACY));
                    } else {
                        holder.tvAiMsg.setText(Html.fromHtml(rawText));
                    }
                }

                // Source Chips UX (e.g. [Reuters], [The Times of India])
                if (msg.getCitations() != null && !msg.getCitations().isEmpty()) {
                    holder.layoutCitations.setVisibility(View.VISIBLE);
                    AskStoryModel.Citation cit = msg.getCitations().get(0);
                    holder.tvCitationBadge.setText("[" + cit.getSourceName() + "]");
                    holder.tvCitationBadge.setOnClickListener(v -> {
                        if (cit.getUrl() != null && !cit.getUrl().isEmpty()) {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(cit.getUrl()));
                            startActivity(intent);
                        }
                    });
                } else {
                    holder.layoutCitations.setVisibility(View.GONE);
                }
            }
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        class ChatViewHolder extends RecyclerView.ViewHolder {
            View layoutUser, layoutAi, layoutCitations;
            TextView tvUserMsg, tvAiMsg, tvCitationBadge;

            public ChatViewHolder(@NonNull View itemView) {
                super(itemView);
                layoutUser = itemView.findViewById(R.id.layout_user_message);
                layoutAi = itemView.findViewById(R.id.layout_ai_message);
                layoutCitations = itemView.findViewById(R.id.layout_citations);
                tvUserMsg = itemView.findViewById(R.id.tv_user_message);
                tvAiMsg = itemView.findViewById(R.id.tv_ai_message);
                tvCitationBadge = itemView.findViewById(R.id.tv_citation_badge);
            }
        }
    }
}
