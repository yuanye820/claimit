package com.claimit.app;

import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final String PREFS = "claimit";
    // Built-in production endpoint. The UI no longer asks the user for a server URL.
    private static final String API_BASE = "https://120.26.146.148/claimit-api";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private SharedPreferences prefs;
    private String token = "";
    private int listId = -1;

    private LinearLayout authScreen;
    private LinearLayout mainScreen;

    private EditText emailInput;
    private EditText passwordInput;
    private EditText displayNameInput;
    private EditText listNameInput;
    private EditText listIdInput;
    private EditText itemNameInput;
    private EditText itemTotalInput;
    private TextView authStatusText;
    private TextView mainStatusText;
    private LinearLayout itemsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        token = prefs.getString("token", "");
        listId = prefs.getInt("listId", -1);
        setContentView(buildUi());
        if (token.isEmpty()) {
            showAuth("Sign in to continue");
        } else {
            showMain("Signed in. Create or load a list.");
        }
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(20));
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("ClaimIt");
        title.setTextSize(28);
        title.setTypeface(null, Typeface.BOLD);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("One shared list. Conflict-safe claims.");
        subtitle.setTextSize(13);
        subtitle.setPadding(0, 0, 0, dp(12));
        root.addView(subtitle);

        authScreen = new LinearLayout(this);
        authScreen.setOrientation(LinearLayout.VERTICAL);
        root.addView(authScreen);

        TextView signIn = new TextView(this);
        signIn.setText("Sign in");
        signIn.setTextSize(20);
        signIn.setTypeface(null, Typeface.BOLD);
        authScreen.addView(signIn);

        emailInput = input("Email", InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        passwordInput = input(
                "Password",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
        );
        displayNameInput = input("Display name", InputType.TYPE_CLASS_TEXT);
        authScreen.addView(label("Email"));
        authScreen.addView(emailInput);
        authScreen.addView(label("Password"));
        authScreen.addView(passwordInput);
        authScreen.addView(label("Display name (register only)"));
        authScreen.addView(displayNameInput);

        LinearLayout authRow = new LinearLayout(this);
        authRow.setOrientation(LinearLayout.HORIZONTAL);
        authRow.addView(button("Login", this::login), weightParams());
        authRow.addView(button("Register", this::register), weightParams());
        authScreen.addView(authRow);

        authStatusText = new TextView(this);
        authStatusText.setTextSize(12);
        authStatusText.setPadding(0, dp(8), 0, 0);
        authScreen.addView(authStatusText);

        mainScreen = new LinearLayout(this);
        mainScreen.setOrientation(LinearLayout.VERTICAL);
        mainScreen.setVisibility(View.GONE);
        root.addView(mainScreen);

        TextView signedIn = new TextView(this);
        signedIn.setText("Shared list");
        signedIn.setTextSize(20);
        signedIn.setTypeface(null, Typeface.BOLD);
        mainScreen.addView(signedIn);

        mainStatusText = new TextView(this);
        mainStatusText.setTextSize(12);
        mainStatusText.setPadding(0, dp(8), 0, dp(8));
        mainScreen.addView(mainStatusText);

        mainScreen.addView(divider());

        listNameInput = input("Trip", InputType.TYPE_CLASS_TEXT);
        listNameInput.setText("Trip");
        mainScreen.addView(label("Create a list"));
        mainScreen.addView(listNameInput);
        mainScreen.addView(button("Create list", this::createList));

        listIdInput = input("List ID", InputType.TYPE_CLASS_NUMBER);
        if (listId > 0) {
            listIdInput.setText(String.valueOf(listId));
        }
        mainScreen.addView(label("List ID (created or existing)"));
        mainScreen.addView(listIdInput);
        mainScreen.addView(button("Load items", this::loadItems));

        itemNameInput = input("Tent", InputType.TYPE_CLASS_TEXT);
        itemNameInput.setText("Tent");
        itemTotalInput = input("1", InputType.TYPE_CLASS_NUMBER);
        itemTotalInput.setText("1");
        mainScreen.addView(label("Add an item"));
        mainScreen.addView(itemNameInput);
        mainScreen.addView(itemTotalInput);
        mainScreen.addView(button("Add item", this::addItem));

        itemsContainer = new LinearLayout(this);
        itemsContainer.setOrientation(LinearLayout.VERTICAL);
        mainScreen.addView(itemsContainer);

        mainScreen.addView(divider());
        mainScreen.addView(button("Sign out", this::clearToken));

        return scroll;
    }

    private void showAuth(String message) {
        authScreen.setVisibility(View.VISIBLE);
        mainScreen.setVisibility(View.GONE);
        authStatusText.setText(message);
    }

    private void showMain(String message) {
        authScreen.setVisibility(View.GONE);
        mainScreen.setVisibility(View.VISIBLE);
        mainStatusText.setText(message);
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
            status("Email and password are required");
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
                        showMain("Signed in. Create or load a list.");
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
            showAuth("Sign in first");
            return;
        }
        final String name = listNameInput.getText().toString().trim();
        if (name.isEmpty()) {
            status("List name is required");
            return;
        }

        JSONObject body = new JSONObject();
        try {
            body.put("name", name);
        } catch (JSONException e) {
            status("JSON error");
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
                        status("List " + listId + " created (code "
                                + json.optString("code") + ")");
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
            showAuth("Sign in first");
            return;
        }
        final int list = currentListId();
        if (list <= 0) {
            status("Create a list or enter a list ID first");
            return;
        }
        final String name = itemNameInput.getText().toString().trim();
        final int total = parseInt(itemTotalInput.getText().toString(), -1);
        if (name.isEmpty() || total <= 0) {
            status("Item name and total > 0 are required");
            return;
        }

        JSONObject body = new JSONObject();
        try {
            body.put("name", name);
            body.put("total", total);
        } catch (JSONException e) {
            status("JSON error");
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
                    try {
                        JSONObject json = new JSONObject(response.body);
                        status("Item " + json.getInt("id") + " added");
                        loadItems();
                    } catch (JSONException e) {
                        status("Bad item response: " + response.body);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> status("Network error: " + e.getMessage()));
            }
        });
    }

    private void loadItems() {
        if (token.isEmpty()) {
            showAuth("Sign in first");
            return;
        }
        final int list = currentListId();
        if (list <= 0) {
            status("Create a list or enter a list ID first");
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
                        itemsContainer.removeAllViews();
                        for (int i = 0; i < array.length(); i++) {
                            itemsContainer.addView(itemRow(array.getJSONObject(i)));
                        }
                        status("Loaded " + array.length() + " item(s)");
                    } catch (JSONException e) {
                        status("Bad items response: " + response.body);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> status("Network error: " + e.getMessage()));
            }
        });
    }

    private View itemRow(JSONObject item) throws JSONException {
        final int id = item.getInt("id");
        final String name = item.getString("name");
        final int remaining = item.getInt("remaining");

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dp(8), 0, dp(8));

        TextView text = new TextView(this);
        text.setText(name + "  |  id " + id + "  |  remaining " + remaining);
        row.addView(text);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.addView(button("Claim 1", () -> claim(id)), weightParams());
        actions.addView(button("Release 1", () -> release(id)), weightParams());
        row.addView(actions);
        return row;
    }

    private void claim(int itemId) {
        if (token.isEmpty()) {
            showAuth("Sign in first");
            return;
        }
        JSONObject body = new JSONObject();
        try {
            body.put("quantity", 1);
        } catch (JSONException e) {
            status("JSON error");
            return;
        }

        status("Claiming item " + itemId + "...");
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
                        status("Claimed item " + itemId);
                        loadItems();
                    } else if (response.code == 409) {
                        status("Conflict: someone else claimed the last unit");
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
            showAuth("Sign in first");
            return;
        }
        JSONObject body = new JSONObject();
        try {
            body.put("quantity", 1);
        } catch (JSONException e) {
            status("JSON error");
            return;
        }

        status("Releasing item " + itemId + "...");
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
                        status("Released item " + itemId);
                        loadItems();
                    } else if (response.code == 409) {
                        status("You have no claim to release");
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
        itemsContainer.removeAllViews();
        passwordInput.setText("");
        showAuth("Signed out");
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

    private EditText input(String hint, int inputType) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setInputType(inputType);
        return input;
    }

    private TextView label(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(12);
        view.setPadding(0, dp(10), 0, dp(2));
        return view;
    }

    private Button button(String text, Runnable action) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setOnClickListener(view -> action.run());
        return button;
    }

    private View divider() {
        View view = new View(this);
        view.setBackgroundColor(0xFFDDD0C8);
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(1)
                );
        params.topMargin = dp(12);
        params.bottomMargin = dp(12);
        view.setLayoutParams(params);
        return view;
    }

    private LinearLayout.LayoutParams weightParams() {
        return new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        );
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}
