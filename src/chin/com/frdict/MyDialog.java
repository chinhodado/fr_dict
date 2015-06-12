package chin.com.frdict;

import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MyDialog extends Activity {
    public static boolean active = false;
    public static Activity myDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.dialog);

        final EditText edt = (EditText) findViewById(R.id.dialog_edt);
        View top = (View) findViewById(R.id.dialog_top);
        final WebView webView = (WebView) findViewById(R.id.webView1);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Toast.makeText(myDialog, description, Toast.LENGTH_SHORT).show();
            }
        });

        myDialog = MyDialog.this;

        edt.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    final String str = edt.getText().toString();
                    if (str.length() > 0) {
                        new AsyncTask<Void, Void, String>(){
                            boolean exceptionOccurred = false;
                            @Override
                            protected String doInBackground(Void... params) {
                                String html = null;
                                try {
                                    html = Jsoup.connect("http://en.wiktionary.org/w/index.php?title=" + str + "&printable=yes")
                                            .ignoreContentType(true).execute().body();
                                } catch (IOException e) {
                                    html = "Error getting word from Wiktionary. Probably 404-ed";
                                    exceptionOccurred = true;
                                    e.printStackTrace();
                                }
                                return html;
                            }

                            @Override
                            protected void onPostExecute(String html) {
                                try {
                                    if (exceptionOccurred) {
                                        webView.loadDataWithBaseURL("", html, "text/html", "UTF-8", "");
                                        return;
                                    }

                                    // parse and do some initial cleaning of the DOM
                                    Document doc = Jsoup.parse(html);
                                    Element content = doc.select("#mw-content-text").first();
                                    content.select("script").remove();               // remove <script> tags
                                    content.select("noscript").remove();             // remove <noscript> tags
                                    content.select("#toc").remove();                 // remove the table of content
                                    removeComments(content);                         // remove comments

                                    // parse for the content of the French section, if it exist
                                    Elements children = content.children();
                                    boolean frenchFound = false;
                                    boolean frenchEndReached = false;
                                    boolean isCurrentlyEtymology = false;
                                    boolean isCurrentlyPronunciation = false;
                                    Elements frenchCollection = new Elements();
                                    Elements etymologyCollection = new Elements();
                                    Elements pronunciationCollection = new Elements();
                                    for (Element elem : children) {
                                        if (!frenchFound) {
                                            if (elem.tagName().equals("h2") && elem.text().equals("French")) {
                                                frenchFound = true;
                                            }
                                            else {
                                                elem.remove();
                                            }
                                        }
                                        else {
                                            if (!elem.tagName().equals("h2") && !(elem.tagName().equals("h3") && elem.text().equals("External links")) && !frenchEndReached) {
                                                // get etylmology and pronunciation sections so that we can move them to the back
                                                // of the page later, instead of having them at the beginning of the page
                                                if (elem.tagName().equals("h3") && elem.text().equals("Etymology")) {
                                                    isCurrentlyEtymology = true;
                                                    isCurrentlyPronunciation = false;
                                                    etymologyCollection.add(elem);
                                                }
                                                else if (elem.tagName().equals("h3") && elem.text().equals("Pronunciation")) {
                                                    isCurrentlyPronunciation = true;
                                                    isCurrentlyEtymology = false;
                                                    pronunciationCollection.add(elem);
                                                }
                                                else if (elem.tagName().equals("h3")) {
                                                    // something other than etymology and pronunciation
                                                    isCurrentlyEtymology = false;
                                                    isCurrentlyPronunciation = false;
                                                    frenchCollection.add(elem);
                                                }
                                                else if (isCurrentlyEtymology) {
                                                    isCurrentlyPronunciation = false;
                                                    etymologyCollection.add(elem);
                                                }
                                                else if (isCurrentlyPronunciation) {
                                                    isCurrentlyEtymology = false;
                                                    pronunciationCollection.add(elem);
                                                }
                                                else {
                                                    isCurrentlyPronunciation = false;
                                                    isCurrentlyEtymology = false;
                                                    frenchCollection.add(elem);
                                                }
                                            }
                                            else {
                                                frenchEndReached = true;
                                                break;
                                            }
                                        }
                                    }

                                    // put pronunciation and etymology sections to the back
                                    frenchCollection.addAll(pronunciationCollection);
                                    frenchCollection.addAll(etymologyCollection);

                                    //webView.getSettings().setJavaScriptEnabled(true);
                                    if (frenchFound) {
                                        webView.loadDataWithBaseURL("http://en.wiktionary.org/w/", frenchCollection.toString(), "text/html", "UTF-8", "");
                                    }
                                    else {
                                        webView.loadDataWithBaseURL("", "Not a French word.", "text/html", "UTF-8", "");
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }.execute();
                    }
                    return true;
                }
                return false;
            }
        });

        top.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                moveTaskToBack(true);
            }
        });
    }

    private static void removeComments(Node node) {
        for (int i = 0; i < node.childNodes().size();) {
            Node child = node.childNode(i);
            if (child.nodeName().equals("#comment"))
                child.remove();
            else {
                removeComments(child);
                i++;
            }
        }
    }

    @Override
    protected void onResume() {
        Log.i(Utility.LogTag, "MyDialog onResume()");
        super.onResume();
        active = true;
    }

    @Override
    protected void onPause() {
        Log.i(Utility.LogTag, "MyDialog onPause()");
        super.onPause();
        active = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(Utility.LogTag, "MyDialog onDestroy()");
        active = false;
    }
}
