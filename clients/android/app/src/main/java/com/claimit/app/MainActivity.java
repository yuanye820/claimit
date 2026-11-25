package com.claimit.app;

import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final String PREFS = "claimit";
    // Built-in production endpoint. The UI no longer asks the user for a server URL.
    private static final String API_BASE = "https://120.26.146.148/claimit-api";

    private static final int PAPER = Color.rgb(247, 244, 240);
    private static final int NAVY = Color.rgb(20, 32, 43);
    private static final int INK = Color.rgb(26, 35, 48);
    private static final int WHITE = Color.rgb(255, 255, 255);
    private static final int ORANGE = Color.rgb(212, 135, 90);
    private static final int SLATE = Color.rgb(91, 101, 115);
    private static final int SOFT = Color.rgb(239, 231, 223);
    private static final int LINE = Color.rgb(222, 211, 202);
    private static final int GREEN = Color.rgb(47, 111, 78);
    private static final int GREEN_BG = Color.rgb(232, 240, 234);
    private static final int RED = Color.rgb(179, 66, 58);
    private static final int RED_BG = Color.rgb(247, 232, 228);

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private SharedPreferences prefs;
    private String token = "";
    private int listId = -1;
    private boolean registerMode = false;

    private LinearLayout authScreen;
    private LinearLayout mainScreen;
    private LinearLayout itemsContainer;

    private TextView authTitle;
    private TextView authSubtitle;
    private TextView authStatusText;
    private TextView mainStatusText;
    private LinearLayout displayNameField;

    private EditText emailInput;
    private EditText passwordInput;
    private EditText displayNameInput;
    private EditText listNameInput;
    private EditText listIdInput;
    private EditText itemNameInput;
    private EditText itemTotalInput;

    private MaterialButton primaryAuthButton;
    private MaterialButton secondaryAuthButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(NAVY);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        token = prefs.getString("token", "");
        listId = prefs.getInt("listId", -1);
        setContentView(buildUi());
        if (token.isEmpty()) {
            showAuth("Sign in to continue.");
        } else {
            showMain("Create or open a list to get started.");
        }
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(PAPER);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(root);

        root.addView(buildHeader());

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(18), dp(20), dp(32));
        root.addView(content, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        authScreen = new LinearLayout(this);
        authScreen.setOrientation(LinearLayout.VERTICAL);
        content.addView(authScreen);

        mainScreen = new LinearLayout(this);
        mainScreen.setOrientation(LinearLayout.VERTICAL);
        mainScreen.setVisibility(View.GONE);
        content.addView(mainScreen);

        buildAuthScreen();
        buildMainScreen();

        return scroll;
    }

    private View buildHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setBackgroundColor(NAVY);
        header.setPadding(dp(22), statusBarHeight() + dp(18), dp(22), dp(22));

        TextView title = text("ClaimIt", 32, WHITE, true);
        header.addView(title);

        TextView subtitle = text("One shared list. Conflict-safe claims.", 13, Color.rgb(201, 210, 218), false);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        subParams.topMargin = dp(6);
        header.addView(subtitle, subParams);
        return header;
    }

    private void buildAuthScreen() {
        MaterialCardView card = card();
        authScreen.addView(card, cardParams());

        LinearLayout body = cardBody(card);
        authTitle = text("Sign in", 24, INK, true);
        body.addView(authTitle);

        authSubtitle = text("Use your email and password to continue.", 13, SLATE, false);
        addWithTop(body, authSubtitle, dp(4));

        emailInput = addField(body, "Email", InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        passwordInput = addField(
                body,
                "Password",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        Field displayName = field("Display name", InputType.TYPE_CLASS_TEXT);
        displayNameInput = displayName.input;
        displayNameField = displayName.container;
        displayNameField.setVisibility(View.GONE);
        addFieldLayout(body, displayNameField);

        primaryAuthButton = primaryButton("Sign in");
        addWithTop(body, primaryAuthButton, dp(8));
        primaryAuthButton.setOnClickListener(v -> {
            if (registerMode) {
                register();
            } else {
                login();
            }
        });

        secondaryAuthButton = softButton("Create account");
        addWithTop(body, secondaryAuthButton, dp(8));
        secondaryAuthButton.setOnClickListener(v -> setAuthMode(!registerMode));

        authStatusText = text("", 12, SLATE, false);
        addWithTop(body, authStatusText, dp(14));

        TextView hint = text("The server address is built in. Just sign in.", 11, SLATE, false);
        addWithTop(body, hint, dp(18));
    }

    private void buildMainScreen() {
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = text("Shared list", 24, INK, true);
        top.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        MaterialButton signOut = smallButton("Sign out", SOFT, INK);
        top.addView(signOut, new LinearLayout.LayoutParams(dp(96), dp(40)));
        signOut.setOnClickListener(v -> clearToken());
        mainScreen.addView(top);

        mainStatusText = text("", 12, SLATE, false);
        addWithTop(mainScreen, mainStatusText, dp(6));

        MaterialCardView listCard = card();
        mainScreen.addView(listCard, cardParams());
        LinearLayout listBody = cardBody(listCard);
        listBody.addView(text("Create or open a list", 18, INK, true));

        listNameInput = addField(listBody, "List name", InputType.TYPE_CLASS_TEXT);
        listNameInput.setText("Trip");

        MaterialButton create = primaryButton("Create list");
        addWithTop(listBody, create, dp(4));
        create.setOnClickListener(v -> createList());

        LinearLayout openRow = new LinearLayout(this);
        openRow.setOrientation(LinearLayout.HORIZONTAL);
        openRow.setGravity(Gravity.BOTTOM);
        LinearLayout.LayoutParams openParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        openParams.topMargin = dp(14);
        listBody.addView(openRow, openParams);

        Field listIdField = field("List ID", InputType.TYPE_CLASS_NUMBER);
        listIdInput = listIdField.input;
        if (listId > 0) {
            listIdInput.setText(String.valueOf(listId));
        }
        LinearLayout.LayoutParams idParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        idParams.rightMargin = dp(10);
        openRow.addView(listIdField.container, idParams);
        MaterialButton load = smallButton("Load items", NAVY, WHITE);
        openRow.addView(load, new LinearLayout.LayoutParams(dp(124), dp(56)));
        load.setOnClickListener(v -> loadItems());

        MaterialCardView addCard = card();
        mainScreen.addView(addCard, cardParams());
        LinearLayout addBody = cardBody(addCard);
        addBody.addView(text("Add an item", 18, INK, true));

        LinearLayout addRow = new LinearLayout(this);
        addRow.setOrientation(LinearLayout.HORIZONTAL);
        addRow.setGravity(Gravity.BOTTOM);
        LinearLayout.LayoutParams addParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        addParams.topMargin = dp(4);
        addBody.addView(addRow, addParams);

        Field itemNameField = field("Item name", InputType.TYPE_CLASS_TEXT);
        itemNameInput = itemNameField.input;
        itemNameInput.setText("Tent");
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f);
        nameParams.rightMargin = dp(10);
        addRow.addView(itemNameField.container, nameParams);

        Field totalField = field("Total", InputType.TYPE_CLASS_NUMBER);
        itemTotalInput = totalField.input;
        itemTotalInput.setText("1");
        addRow.addView(totalField.container, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        MaterialButton add = primaryButton("Add item");
        addWithTop(addBody, add, dp(4));
        add.setOnClickListener(v -> addItem());

        TextView itemsTitle = text("Items", 20, INK, true);
        addWithTop(mainScreen, itemsTitle, dp(22));

        itemsContainer = new LinearLayout(this);
        itemsContainer.setOrientation(LinearLayout.VERTICAL);
        addWithTop(mainScreen, itemsContainer, dp(10));
        itemsContainer.addView(text("Open a list to see its items.", 13, SLATE, false));
    }

    private void setAuthMode(boolean register) {
        registerMode = register;
        authTitle.setText(register ? "Create account" : "Sign in");
        authSubtitle.setText(register
                ? "Create an account to start a shared list."
                : "Use your email and password to continue.");
        displayNameField.setVisibility(register ? View.VISIBLE : View.GONE);
        primaryAuthButton.setText(register ? "Create account" : "Sign in");
        secondaryAuthButton.setText(register ? "Back to sign in" : "Create account");
        status("");
    }

    private void showAuth(String message) {
        authScreen.setVisibility(View.VISIBLE);
        mainScreen.setVisibility(View.GONE);
        if (authStatusText != null) {
            authStatusText.setText(message);
        }
    }

    private void showMain(String message) {
        authScreen.setVisibility(View.GONE);
        mainScreen.setVisibility(View.VISIBLE);
        if (mainStatusText != null) {
            mainStatusText.setText(message);
        }
    }

    private void register() {
        auth("/auth/register", true);
    }

    private void login() {
        auth("/auth/login", false);
    }

    private void auth(String path, boolean includeDisplayName) {
        final String email = emailInput.getText().toString().trim();
        final String password = passwordInput.getText().toString();
        final String displayName = displayNameInput.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            status("Email and password are required.");
            return;
        }

        JSONObject body = new JSONObject();
        try {
            body.put("email", email);
            body.put("password", password);
            if (includeDisplayName) {
                body.put("display_name", displayName.isEmpty() ? email : displayName);
            }
        } catch (JSONException e) {
            status("JSON error: " + e.getMessage());
            return;
        }

        status("Signing in...");
        executor.execute(() -> {
            try {
                ApiClient.Response response =
                        ApiClient.request(API_BASE, "POST", path, null, body.toString());
                runOnUiThread(() -> {
                    if (!response.ok()) {
                        status("HTTP " + response.code + ": " + response.body);
                        return;
                    }
                    try {
                        JSONObject json = new JSONObject(response.body);
                        token = json.getString("access_token");
                        prefs.edit().putString("token", token).apply();
                        passwordInput.setText("");
                        setAuthMode(false);
                        showMain("Signed in. Create or open a list.");
                    } catch (JSONException e) {
                        status("Bad token response: " + response.body);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> status("Network error: " + e.getMessage()));
            }
        });
    }

    private void createList() {
        if (token.isEmpty()) {
            showAuth("Sign in first.");
            return;
        }
        final String name = listNameInput.getText().toString().trim();
        if (name.isEmpty()) {
            status("List name is required.");
            return;
        }

        JSONObject body = new JSONObject();
        try {
            body.put("name", name);
        } catch (JSONException e) {
            status("JSON error.");
            return;
        }

        status("Creating list...");
        executor.execute(() -> {
            try {
                ApiClient.Response response =
                        ApiClient.request(API_BASE, "POST", "/lists", token, body.toString());
                runOnUiThread(() -> {
                    if (!response.ok()) {
                        status("HTTP " + response.code + ": " + response.body);
                        return;
                    }
                    try {
                        JSONObject json = new JSONObject(response.body);
                        listId = json.getInt("id");
                        prefs.edit().putInt("listId", listId).apply();
                        listIdInput.setText(String.valueOf(listId));
                        status("List " + listId + " created. Add an item next.");
                    } catch (JSONException e) {
                        status("Bad list response: " + response.body);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> status("Network error: " + e.getMessage()));
            }
        });
    }

    private void addItem() {
        if (token.isEmpty()) {
            showAuth("Sign in first.");
            return;
        }
        final int list = currentListId();
        if (list <= 0) {
            status("Open a list first.");
            return;
        }
        final String name = itemNameInput.getText().toString().trim();
        final int total = parseInt(itemTotalInput.getText().toString(), -1);
        if (name.isEmpty() || total <= 0) {
            status("Item name and total must be positive.");
            return;
        }

        JSONObject body = new JSONObject();
        try {
            body.put("name", name);
            body.put("total", total);
        } catch (JSONException e) {
            status("JSON error.");
            return;
        }

        status("Adding item...");
        executor.execute(() -> {
            try {
                ApiClient.Response response = ApiClient.request(
                        API_BASE,
                        "POST",
                        "/lists/" + list + "/items",
                        token,
                        body.toString()
                );
                runOnUiThread(() -> {
                    if (!response.ok()) {
                        status("HTTP " + response.code + ": " + response.body);
                        return;
                    }
                    loadItems();
                });
            } catch (Exception e) {
                runOnUiThread(() -> status("Network error: " + e.getMessage()));
            }
        });
    }

    private void loadItems() {
        if (token.isEmpty()) {
            showAuth("Sign in first.");
            return;
        }
        final int list = currentListId();
        if (list <= 0) {
            status("Open a list first.");
            return;
        }

        status("Loading items...");
        executor.execute(() -> {
            try {
                ApiClient.Response response = ApiClient.request(
                        API_BASE,
                        "GET",
                        "/lists/" + list + "/items",
                        token,
                        null
                );
                runOnUiThread(() -> {
                    if (!response.ok()) {
                        status("HTTP " + response.code + ": " + response.body);
                        return;
                    }
                    try {
                        JSONArray array = new JSONArray(response.body);
                        renderItems(array);
                        status("Loaded " + array.length() + " item(s).");
                    } catch (JSONException e) {
                        status("Bad items response: " + response.body);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> status("Network error: " + e.getMessage()));
            }
        });
    }

    private void renderItems(JSONArray array) throws JSONException {
        itemsContainer.removeAllViews();
        if (array.length() == 0) {
            itemsContainer.addView(text("No items yet. Add the first one above.", 13, SLATE, false));
            return;
        }
        for (int i = 0; i < array.length(); i++) {
            itemsContainer.addView(itemRow(array.getJSONObject(i)), cardParams());
        }
    }

    private View itemRow(JSONObject item) throws JSONException {
        final int id = item.getInt("id");
        final String name = item.getString("name");
        final int remaining = item.getInt("remaining");
        final int total = item.getInt("total");

        MaterialCardView card = card();
        card.setCardElevation(dp(0));
        card.setStrokeColor(ColorStateList.valueOf(LINE));
        card.setStrokeWidth(dp(1));
        LinearLayout body = cardBody(card);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(text(name, 17, INK, true),
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView badge = text(remaining > 0 ? remaining + " left" : "full", 11,
                remaining > 0 ? GREEN : RED, true);
        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setColor(remaining > 0 ? GREEN_BG : RED_BG);
        badgeBg.setCornerRadius(dp(20));
        badge.setBackground(badgeBg);
        badge.setPadding(dp(9), dp(4), dp(9), dp(4));
        top.addView(badge);

        body.addView(top);
        TextView meta = text("id " + id + "  ·  remaining " + remaining + " / " + total, 12, SLATE, false);
        addWithTop(body, meta, dp(4));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        actionParams.topMargin = dp(12);
        body.addView(actions, actionParams);

        MaterialButton claim = smallButton("Claim 1", ORANGE, WHITE);
        LinearLayout.LayoutParams claimParams = new LinearLayout.LayoutParams(0, dp(44), 1f);
        claimParams.rightMargin = dp(8);
        actions.addView(claim, claimParams);
        claim.setOnClickListener(v -> claim(id));

        MaterialButton release = smallButton("Release 1", SOFT, INK);
        actions.addView(release, new LinearLayout.LayoutParams(0, dp(44), 1f));
        release.setOnClickListener(v -> release(id));

        return card;
    }

    private void claim(int itemId) {
        if (token.isEmpty()) {
            showAuth("Sign in first.");
            return;
        }
        JSONObject body = new JSONObject();
        try {
            body.put("quantity", 1);
        } catch (JSONException e) {
            status("JSON error.");
            return;
        }

        status("Claiming...");
        executor.execute(() -> {
            try {
                ApiClient.Response response = ApiClient.request(
                        API_BASE,
                        "POST",
                        "/items/" + itemId + "/claims",
                        token,
                        body.toString()
                );
                runOnUiThread(() -> {
                    if (response.ok()) {
                        loadItems();
                    } else if (response.code == 409) {
                        status("Conflict: someone else claimed the last unit.");
                        loadItems();
                    } else {
                        status("HTTP " + response.code + ": " + response.body);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> status("Network error: " + e.getMessage()));
            }
        });
    }

    private void release(int itemId) {
        if (token.isEmpty()) {
            showAuth("Sign in first.");
            return;
        }
        JSONObject body = new JSONObject();
        try {
            body.put("quantity", 1);
        } catch (JSONException e) {
            status("JSON error.");
            return;
        }

        status("Releasing...");
        executor.execute(() -> {
            try {
                ApiClient.Response response = ApiClient.request(
                        API_BASE,
                        "POST",
                        "/items/" + itemId + "/releases",
                        token,
                        body.toString()
                );
                runOnUiThread(() -> {
                    if (response.ok()) {
                        loadItems();
                    } else if (response.code == 409) {
                        status("You have no claim to release.");
                        loadItems();
                    } else {
                        status("HTTP " + response.code + ": " + response.body);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> status("Network error: " + e.getMessage()));
            }
        });
    }

    private void clearToken() {
        token = "";
        prefs.edit().remove("token").apply();
        if (itemsContainer != null) {
            itemsContainer.removeAllViews();
        }
        if (passwordInput != null) {
            passwordInput.setText("");
        }
        setAuthMode(false);
        showAuth("Signed out.");
    }

    private int currentListId() {
        String raw = listIdInput.getText().toString().trim();
        if (raw.isEmpty()) {
            return listId;
        }
        return parseInt(raw, -1);
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private void status(String message) {
        if (mainScreen != null && mainScreen.getVisibility() == View.VISIBLE) {
            mainStatusText.setText(message);
        } else if (authStatusText != null) {
            authStatusText.setText(message);
        }
    }

    private MaterialCardView card() {
        MaterialCardView card = new MaterialCardView(this);
        card.setCardBackgroundColor(WHITE);
        card.setRadius(dp(18));
        card.setCardElevation(dp(2));
        card.setStrokeWidth(0);
        return card;
    }

    private LinearLayout cardBody(MaterialCardView card) {
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.addView(body, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        return body;
    }

    private LinearLayout.LayoutParams cardParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = dp(14);
        return params;
    }

    private Field field(String hint, int inputType) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);

        TextView label = text(hint, 11, SLATE, true);
        label.setPadding(dp(2), 0, 0, dp(5));
        container.addView(label);

        EditText input = new EditText(this);
        input.setInputType(inputType);
        input.setTextSize(16);
        input.setSingleLine(true);
        input.setTextColor(INK);
        input.setHintTextColor(SLATE);
        input.setPadding(dp(14), dp(12), dp(14), dp(12));
        input.setMinHeight(dp(52));
        input.setBackground(fieldBackground(false));
        input.setOnFocusChangeListener((view, focused) ->
                view.setBackground(fieldBackground(focused)));
        container.addView(input, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        return new Field(container, input);
    }

    private GradientDrawable fieldBackground(boolean focused) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(WHITE);
        background.setCornerRadius(dp(12));
        background.setStroke(dp(focused ? 2 : 1), focused ? ORANGE : LINE);
        return background;
    }

    private EditText addField(LinearLayout parent, String hint, int inputType) {
        Field field = field(hint, inputType);
        addFieldLayout(parent, field.container);
        return field.input;
    }

    private void addFieldLayout(LinearLayout parent, View layout) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dp(12);
        parent.addView(layout, params);
    }

    private MaterialButton primaryButton(String label) {
        MaterialButton button = baseButton(label, ORANGE, WHITE);
        button.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(52)
        ));
        return button;
    }

    private MaterialButton softButton(String label) {
        MaterialButton button = baseButton(label, SOFT, INK);
        button.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48)
        ));
        return button;
    }

    private MaterialButton smallButton(String label, int background, int foreground) {
        MaterialButton button = baseButton(label, background, foreground);
        button.setTextSize(12);
        button.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(44)
        ));
        return button;
    }

    private MaterialButton baseButton(String label, int background, int foreground) {
        MaterialButton button = new MaterialButton(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(15);
        button.setTypeface(null, Typeface.BOLD);
        button.setTextColor(foreground);
        button.setBackgroundTintList(ColorStateList.valueOf(background));
        button.setCornerRadius(dp(12));
        button.setInsetTop(0);
        button.setInsetBottom(0);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setPadding(dp(14), 0, dp(14), 0);
        return button;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) {
            view.setTypeface(null, Typeface.BOLD);
        }
        return view;
    }

    private void addWithTop(LinearLayout parent, View child, int top) {
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        if (child.getLayoutParams() instanceof LinearLayout.LayoutParams) {
            height = ((LinearLayout.LayoutParams) child.getLayoutParams()).height;
        }
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                height
        );
        params.topMargin = top;
        parent.addView(child, params);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private int statusBarHeight() {
        int id = getResources().getIdentifier("status_bar_height", "dimen", "android");
        return id > 0
                ? getResources().getDimensionPixelSize(id)
                : dp(24);
    }

    private static class Field {
        final LinearLayout container;
        final EditText input;

        Field(LinearLayout container, EditText input) {
            this.container = container;
            this.input = input;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}
