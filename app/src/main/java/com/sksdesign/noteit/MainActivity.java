package com.sksdesign.noteit;

import android.app.*;
import android.os.*;
import android.content.*;
import android.net.*;
import android.view.*;
import android.widget.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.text.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.zip.*;
import org.json.*;

public class MainActivity extends Activity {
    final int PURPLE = Color.rgb(132, 69, 255);
    final int BG = Color.rgb(4, 4, 9);
    final int PANEL = Color.argb(105, 36, 28, 58);
    final int PANEL_STRONG = Color.argb(158, 42, 32, 68);
    final int TEXT = Color.rgb(248, 246, 255);
    final int MUTED = Color.rgb(177, 166, 204);
    static final String APP_VERSION = "2.0.5";
    static final String GITHUB_URL = "https://github.com/complicatiion/NOTEit_app";

    ArrayList<Notebook> notebooks = new ArrayList<>();
    ArrayList<Note> notes = new ArrayList<>();
    String selectedNotebookId = "default";
    LinearLayout root;
    GridLayout grid;
    EditText search, editTitle, editor;
    Note current;
    File store;
    float touchDownX = 0;
    int screen = 0; // 0 notes, 1 notebooks

    static final int REQ_EXPORT_NOTE_ONE = 12, REQ_IMAGE = 14;
    static final int REQ_EXPORT_ALL = 21, REQ_EXPORT_NOTES_ZIP = 22, REQ_EXPORT_BOOKS_ZIP = 23;
    static final int REQ_EXPORT_NOTES_MD = 24, REQ_EXPORT_NOTES_TXT = 25, REQ_EXPORT_NOTES_CSV = 26;
    static final int REQ_EXPORT_BOOKS_CSV = 27;
    static final int REQ_IMPORT_ALL = 31, REQ_IMPORT_NOTES = 32, REQ_IMPORT_BOOKS = 33;

    public void onCreate(Bundle b) {
        super.onCreate(b);
        store = new File(getFilesDir(), "noteit-store.json");
        load();
        showHome(0);
    }

    void base(String heading) {
        FrameLayout stage = new FrameLayout(this);
        stage.setBackground(new GlowBackground());
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(28));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));
        stage.addView(scroll, new FrameLayout.LayoutParams(-1, -1));
        setContentView(stage);
        stage.setOnTouchListener((v, e) -> {
            if (current != null) return false;
            if (e.getAction() == android.view.MotionEvent.ACTION_DOWN) { touchDownX = e.getX(); return true; }
            if (e.getAction() == android.view.MotionEvent.ACTION_UP) {
                float dx = e.getX() - touchDownX;
                if (Math.abs(dx) > dp(90)) { showHome(dx < 0 ? 1 : 0); return true; }
            }
            return true;
        });

        LinearLayout hero = new LinearLayout(this);
        hero.setGravity(Gravity.CENTER_VERTICAL);
        hero.setPadding(dp(14), dp(12), dp(14), dp(12));
        hero.setBackground(glass(PANEL, dp(30), true));
        hero.setElevation(dp(8));

        ImageView logo = new ImageView(this);
        logo.setImageResource(getResources().getIdentifier("ic_launcher", "mipmap", getPackageName()));
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        hero.addView(logo, new LinearLayout.LayoutParams(dp(58), dp(58)));

        TextView title = text(heading, 28, TEXT, true);
        title.setPadding(dp(12), 0, 0, 0);
        hero.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        TextView version = text("v" + APP_VERSION, 12, TEXT, true);
        version.setGravity(Gravity.CENTER);
        version.setPadding(dp(12), dp(6), dp(12), dp(6));
        version.setBackground(glass(Color.argb(95, 132, 69, 255), dp(18), true));
        hero.addView(version, new LinearLayout.LayoutParams(-2, dp(34)));
        root.addView(hero, lp(-1, -2, 0, 0, 0, dp(14)));
    }

    TextView text(String s, int sp, int color, boolean bold) {
        TextView v = new TextView(this); v.setText(s); v.setTextColor(color); v.setTextSize(sp); v.setIncludeFontPadding(true); if (bold) v.setTypeface(Typeface.DEFAULT_BOLD); return v;
    }
    TextView pill(String label, int h) {
        TextView v = text(label, 15, TEXT, true); v.setGravity(Gravity.CENTER); v.setPadding(dp(18), dp(10), dp(18), dp(10)); v.setMinHeight(dp(h)); v.setClickable(true); v.setFocusable(true); v.setBackground(glass(Color.argb(160, 113, 61, 210), dp(28), true)); v.setElevation(dp(6)); return v;
    }
    TextView smallPill(String label) { TextView v = pill(label, 44); v.setTextSize(13); return v; }
    GradientDrawable glass(int color, int radius, boolean accent) { GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{color, Color.argb(56,255,255,255)}); g.setCornerRadius(radius); g.setStroke(dp(1), accent ? Color.argb(190,190,140,255) : Color.argb(75,255,255,255)); return g; }
    LinearLayout.LayoutParams lp(int w,int h,int l,int t,int r,int b){ LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h); p.setMargins(l,t,r,b); return p; }

    void showHome(int tab) {
        current = null; editor = null; screen = tab;
        base("Noteit");
        addTabs();
        addSearchAndActions();
        TextView section = text(tab == 0 ? selectedBook().title + " Notes" : "Notebooks", 18, TEXT, true);
        root.addView(section, lp(-1, -2, dp(4), 0, 0, dp(8)));
        grid = new GridLayout(this); grid.setColumnCount(2); root.addView(grid, new LinearLayout.LayoutParams(-1, -2));
        renderGrid();
    }

    void addTabs() {
        LinearLayout tabs = new LinearLayout(this); tabs.setOrientation(LinearLayout.HORIZONTAL); tabs.setPadding(dp(4), dp(4), dp(4), dp(4)); tabs.setBackground(glass(Color.argb(70,255,255,255), dp(28), false));
        TextView n = pill("Notes", 46); TextView b = pill("Notebooks", 46);
        n.setAlpha(screen==0?1f:.58f); b.setAlpha(screen==1?1f:.58f);
        tabs.addView(n, new LinearLayout.LayoutParams(0, dp(48), 1)); tabs.addView(space(), new LinearLayout.LayoutParams(dp(8), 1)); tabs.addView(b, new LinearLayout.LayoutParams(0, dp(48), 1));
        root.addView(tabs, lp(-1, -2, 0, 0, 0, dp(14)));
        n.setOnClickListener(v -> showHome(0)); b.setOnClickListener(v -> showHome(1));
    }

    void addSearchAndActions() {
        LinearLayout searchBox = new LinearLayout(this); searchBox.setGravity(Gravity.CENTER_VERTICAL); searchBox.setPadding(dp(16),0,dp(16),0); searchBox.setBackground(glass(Color.argb(90,255,255,255), dp(28), false));
        ImageView mag = new ImageView(this); mag.setImageResource(R.drawable.ic_search_24); mag.setColorFilter(TEXT); searchBox.addView(mag, new LinearLayout.LayoutParams(dp(24), dp(24)));
        search = new EditText(this); search.setHint(""); search.setTextColor(TEXT); search.setSingleLine(true); search.setTextSize(15); search.setBackgroundColor(Color.TRANSPARENT); search.setPadding(dp(12),0,0,0);
        searchBox.addView(search, new LinearLayout.LayoutParams(0, dp(54), 1));
        root.addView(searchBox, lp(-1, dp(54), 0, 0, 0, dp(14)));
        LinearLayout actions = new LinearLayout(this); actions.setOrientation(LinearLayout.HORIZONTAL);
        TextView add = pill(screen==0 ? "+   New Note" : "+   New", 54); add.setTextSize(17);
        TextView set = pill("⚙   Settings", 54); set.setTextSize(17);
        actions.addView(add, new LinearLayout.LayoutParams(0, dp(58), 1)); actions.addView(space(), new LinearLayout.LayoutParams(dp(12), 1)); actions.addView(set, new LinearLayout.LayoutParams(0, dp(58), 1));
        root.addView(actions, lp(-1, -2, 0, 0, 0, dp(18)));
        add.setOnClickListener(v -> { if (screen==0) newNote(); else askNotebookName(null); });
        set.setOnClickListener(v -> showSettings());
        search.addTextChangedListener(new TextWatcher(){ public void beforeTextChanged(CharSequence s,int st,int c,int a){} public void onTextChanged(CharSequence s,int st,int b,int c){renderGrid();} public void afterTextChanged(Editable e){} });
    }

    void renderGrid() {
        grid.removeAllViews(); String q = search==null?"":search.getText().toString().toLowerCase(Locale.US).trim(); int count=0;
        if (screen == 0) {
            for (Note n: notes) { if (!n.notebookId.equals(selectedNotebookId)) continue; if (!(n.title+" "+n.body).toLowerCase(Locale.US).contains(q)) continue; addGridCard(noteCard(n)); count++; }
            if (count == 0) addEmpty("No notes in this notebook yet.");
        } else {
            for (Notebook b: notebooks) { if (!b.title.toLowerCase(Locale.US).contains(q)) continue; addGridCard(bookCard(b)); count++; }
            if (count == 0) addEmpty("No notebooks yet.");
        }
    }
    void addGridCard(View card){ GridLayout.LayoutParams gp = new GridLayout.LayoutParams(); gp.width=0; gp.height=dp(screen==1?118:174); gp.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f); gp.setMargins(dp(5),dp(6),dp(5),dp(10)); grid.addView(card,gp); }
    void addEmpty(String msg){ TextView empty=text(msg,15,MUTED,false); empty.setGravity(Gravity.CENTER); empty.setPadding(dp(20),dp(30),dp(20),dp(30)); empty.setBackground(glass(Color.argb(64,255,255,255),dp(30),false)); GridLayout.LayoutParams gp=new GridLayout.LayoutParams(); gp.width=-1; gp.height=-2; gp.columnSpec=GridLayout.spec(0,2); gp.setMargins(0,dp(8),0,0); grid.addView(empty,gp); }

    LinearLayout noteCard(Note n) {
        LinearLayout card=cardBase(accentForBook(n.notebookId)); TextView title=text(n.title.length()==0?"Untitled note":n.title,16,TEXT,true); title.setMaxLines(2); title.setEllipsize(TextUtils.TruncateAt.END); TextView body=text(preview(n.body),12,MUTED,false); body.setMaxLines(4); body.setEllipsize(TextUtils.TruncateAt.END); TextView date=text(fmt(n.updated),11,Color.rgb(203,187,236),false);
        card.addView(title,lp(-1,-2,0,0,0,dp(8))); card.addView(body,new LinearLayout.LayoutParams(-1,0,1)); card.addView(date); card.setOnClickListener(v -> showEditor(n)); return card;
    }
    LinearLayout bookCard(Notebook b) {
        LinearLayout card=cardBase(b.color); TextView title=text(b.title,16,TEXT,true); TextView body=text(countNotes(b.id)+" notes",12,MUTED,false); TextView date=text(fmt(b.updated),10,Color.rgb(203,187,236),false);
        card.addView(title,lp(-1,-2,0,0,0,dp(6))); card.addView(body,new LinearLayout.LayoutParams(-1,0,1)); card.addView(date); card.setOnClickListener(v -> { selectedNotebookId=b.id; showHome(0); }); card.setOnLongClickListener(v -> { manageNotebook(b); return true; }); return card;
    }
    LinearLayout cardBase(int strokeColor){ LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(dp(15),dp(14),dp(15),dp(14)); card.setBackground(glassStroke(PANEL_STRONG,dp(28),strokeColor)); card.setClickable(true); card.setFocusable(true); card.setElevation(dp(7)); return card; }

    void newNote(){ Note n=new Note(); n.notebookId=selectedNotebookId; n.title=""; notes.add(0,n); save(); showEditor(n); }
    void askNotebookName(Notebook edit){ final EditText input=new EditText(this); input.setHint("Notebook title"); input.setSingleLine(true); input.setTextColor(Color.BLACK); if(edit!=null) input.setText(edit.title); new AlertDialog.Builder(this).setTitle(edit==null?"New notebook":"Rename notebook").setView(input).setPositiveButton("Save",(d,w)->{ String t=input.getText().toString().trim(); if(t.length()==0)t="Untitled notebook"; if(edit==null){ Notebook b=new Notebook(); b.title=t; b.color=nextNotebookColor(); notebooks.add(0,b); selectedNotebookId=b.id; } else { edit.title=t; edit.updated=System.currentTimeMillis(); } save(); showHome(edit==null?0:1); }).setNegativeButton("Cancel",null).show(); }

    void manageNotebook(Notebook b){
        String[] labels=new String[notes.size()]; boolean[] checked=new boolean[notes.size()];
        for(int i=0;i<notes.size();i++){ Note n=notes.get(i); labels[i]=(n.title.length()==0?"Untitled note":n.title); checked[i]=b.id.equals(n.notebookId); }
        new AlertDialog.Builder(this).setTitle(b.title).setMultiChoiceItems(labels,checked,(d,which,isChecked)->{ notes.get(which).notebookId=isChecked?b.id:fallbackNotebookId(b.id); notes.get(which).updated=System.currentTimeMillis(); })
            .setPositiveButton("Save",(d,w)->{ b.updated=System.currentTimeMillis(); save(); showHome(1); })
            .setNeutralButton("Rename",(d,w)->askNotebookName(b))
            .setNegativeButton("Cancel",null).show();
    }

    void showEditor(Note n) {
        current=n; base("Editor");
        editTitle=new EditText(this); editTitle.setHint("Note title"); editTitle.setHintTextColor(MUTED); editTitle.setText(n.title); editTitle.setTextColor(TEXT); editTitle.setTextSize(23); editTitle.setSingleLine(true); editTitle.setPadding(dp(18),0,dp(18),0); editTitle.setBackground(glass(Color.argb(80,255,255,255),dp(28),false)); root.addView(editTitle,lp(-1,dp(58),0,0,0,dp(10)));
        TextView bookPick=smallPill("Notebook: "+bookTitle(n.notebookId)); bookPick.setGravity(Gravity.CENTER_VERTICAL); bookPick.setPadding(dp(18),0,dp(18),0); bookPick.setBackground(glassStroke(Color.argb(105,255,255,255),dp(24),accentForBook(n.notebookId))); root.addView(bookPick,lp(-1,dp(48),0,0,0,dp(12))); bookPick.setOnClickListener(v->chooseNotebook(bookPick));
        HorizontalScrollView hsv=new HorizontalScrollView(this); hsv.setHorizontalScrollBarEnabled(false); LinearLayout bar=new LinearLayout(this); bar.setOrientation(LinearLayout.HORIZONTAL);
        String[] tools={"H1","H2","List","Task","Link","Image","Paste"}; for(String t:tools){ TextView b=smallPill(t); LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-2,dp(46)); bp.setMargins(0,0,dp(10),0); bar.addView(b,bp); b.setOnClickListener(v->tool(((TextView)v).getText().toString())); }
        hsv.addView(bar); root.addView(hsv,lp(-1,dp(50),0,0,0,dp(12)));
        editor=new EditText(this); editor.setText(n.body); editor.setTextColor(TEXT); editor.setHintTextColor(MUTED); editor.setHint("Start writing..."); editor.setGravity(Gravity.TOP|Gravity.START); editor.setMinLines(18); editor.setTextSize(16); editor.setLineSpacing(dp(2),1.08f); editor.setPadding(dp(18),dp(18),dp(18),dp(18)); editor.setBackground(glass(Color.argb(86,255,255,255),dp(30),false)); root.addView(editor,lp(-1,-2,0,0,0,dp(14)));
        LinearLayout bottom=new LinearLayout(this); bottom.setOrientation(LinearLayout.HORIZONTAL);
        ImageButton del=iconBtn(R.drawable.ic_delete_24); ImageButton exp=iconBtn(R.drawable.ic_download_24); ImageButton ok=iconBtn(R.drawable.ic_check_24); ImageButton save=iconBtn(R.drawable.ic_save_24);
        bottom.addView(del,new LinearLayout.LayoutParams(0,dp(54),1)); bottom.addView(space(),new LinearLayout.LayoutParams(dp(10),1)); bottom.addView(exp,new LinearLayout.LayoutParams(0,dp(54),1)); bottom.addView(space(),new LinearLayout.LayoutParams(dp(10),1)); bottom.addView(ok,new LinearLayout.LayoutParams(0,dp(54),1)); bottom.addView(space(),new LinearLayout.LayoutParams(dp(10),1)); bottom.addView(save,new LinearLayout.LayoutParams(0,dp(54),1)); root.addView(bottom);
        ok.setOnClickListener(v->{commit(); showHome(0);}); save.setOnClickListener(v->{commit(); toast("Saved");}); exp.setOnClickListener(v->{commit(); createDoc("text/markdown", safe(current.title.length()==0?"note":current.title)+".md", REQ_EXPORT_NOTE_ONE);}); del.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Delete note?").setMessage(current.title.length()==0?"Untitled note":current.title).setPositiveButton("Delete",(d,w)->{notes.remove(current);save();showHome(0);}).setNegativeButton("Cancel",null).show());
    }
    ImageButton iconBtn(int res){ ImageButton b=new ImageButton(this); b.setImageResource(res); b.setColorFilter(TEXT); b.setPadding(dp(14),dp(14),dp(14),dp(14)); b.setBackground(glass(Color.argb(160,113,61,210),dp(28),true)); b.setScaleType(ImageView.ScaleType.CENTER); b.setElevation(dp(6)); return b; }
    Space space(){ return new Space(this); }

    void tool(String t){ int s=Math.max(0,editor.getSelectionStart()); int e=Math.max(s,editor.getSelectionEnd()); String selected=editor.getText().subSequence(s,e).toString(); String ins=""; if(t.equals("H1"))ins="# "+(selected.length()>0?selected:"Heading"); else if(t.equals("H2"))ins="## "+(selected.length()>0?selected:"Subheading"); else if(t.equals("List"))ins="- "+(selected.length()>0?selected:"List item"); else if(t.equals("Task"))ins="- [ ] "+(selected.length()>0?selected:"Task"); else if(t.equals("Link")){ linkDialog(); return; } else if(t.equals("Image")){ Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.setType("image/*"); i.addCategory(Intent.CATEGORY_OPENABLE); startActivityForResult(i,REQ_IMAGE); return; } else if(t.equals("Paste")){ android.content.ClipboardManager cm=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE); if(cm!=null&&cm.hasPrimaryClip())ins=String.valueOf(cm.getPrimaryClip().getItemAt(0).coerceToText(this)); } insert(ins); }
    void linkDialog(){ LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); final EditText label=new EditText(this); final EditText url=new EditText(this); label.setHint("Link text"); url.setHint("https://example.com"); box.addView(label); box.addView(url); new AlertDialog.Builder(this).setTitle("Insert link").setView(box).setPositiveButton("Insert",(d,w)->{String l=label.getText().toString().trim();String u=url.getText().toString().trim(); if(l.length()==0)l="Link"; if(u.length()==0)u="https://"; insert("["+l+"]("+u+")");}).setNegativeButton("Cancel",null).show(); }
    void insert(String s){ if(s==null||s.length()==0)return; int p=Math.max(0,editor.getSelectionStart()); editor.getText().insert(p,s+"\n"); }
    void commit(){ if(current==null||editTitle==null||editor==null)return; current.title=editTitle.getText().toString().trim(); current.body=editor.getText().toString(); current.updated=System.currentTimeMillis(); Notebook b=bookById(current.notebookId); if(b!=null)b.updated=current.updated; save(); }

    void showSettings(){ current=null; editor=null; base("Settings"); addMenu("Export", new String[][]{{"Export all","export_all"},{"Export notes ZIP","export_notes_zip"},{"Export notes MD","export_notes_md"},{"Export notes TXT","export_notes_txt"},{"Export notes CSV","export_notes_csv"},{"Export notebooks ZIP","export_books_zip"},{"Export notebooks CSV","export_books_csv"}}); addMenu("Import", new String[][]{{"Import all","import_all"},{"Import notes","import_notes"},{"Import notebooks","import_books"}}); String[] rows={"Clear app cache","Reset all notes","App info"}; for(String r:rows){ TextView row=pill(r,58); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(dp(20),0,dp(20),0); root.addView(row,lp(-1,dp(60),0,0,0,dp(12))); row.setOnClickListener(v->settingsAction(((TextView)v).getText().toString())); } TextView back=pill("Back",50); root.addView(back,lp(-1,dp(52),0,dp(4),0,0)); back.setOnClickListener(v->showHome(0)); }
    void addMenu(String title, String[][] actions){ TextView toggle=pill(title,58); toggle.setGravity(Gravity.CENTER_VERTICAL); toggle.setPadding(dp(20),0,dp(20),0); root.addView(toggle,lp(-1,dp(60),0,0,0,dp(10))); LinearLayout menu=menuCard(); for(String[] a:actions)addMenuButton(menu,a[0],a[1]); menu.setVisibility(View.GONE); root.addView(menu,lp(-1,-2,0,0,0,dp(12))); toggle.setOnClickListener(v->menu.setVisibility(menu.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE)); }
    LinearLayout menuCard(){ LinearLayout m=new LinearLayout(this); m.setOrientation(LinearLayout.VERTICAL); m.setPadding(dp(12),dp(12),dp(12),dp(4)); m.setBackground(glass(Color.argb(85,255,255,255),dp(28),true)); m.setElevation(dp(6)); return m; }
    void addMenuButton(LinearLayout menu,String label,String action){ TextView b=pill(label,46); b.setGravity(Gravity.CENTER_VERTICAL); b.setPadding(dp(18),0,dp(18),0); b.setTag(action); menu.addView(b,lp(-1,dp(48),0,0,0,dp(8))); b.setOnClickListener(v->settingsAction(String.valueOf(v.getTag()))); }
    void settingsAction(String s){ if(s.equals("export_all"))createDoc("application/zip","noteit-all.zip",REQ_EXPORT_ALL); else if(s.equals("export_notes_zip"))createDoc("application/zip","noteit-notes.zip",REQ_EXPORT_NOTES_ZIP); else if(s.equals("export_books_zip"))createDoc("application/zip","noteit-notebooks.zip",REQ_EXPORT_BOOKS_ZIP); else if(s.equals("export_notes_md"))createDoc("text/markdown","noteit-notes.md",REQ_EXPORT_NOTES_MD); else if(s.equals("export_notes_txt"))createDoc("text/plain","noteit-notes.txt",REQ_EXPORT_NOTES_TXT); else if(s.equals("export_notes_csv"))createDoc("text/csv","noteit-notes.csv",REQ_EXPORT_NOTES_CSV); else if(s.equals("export_books_csv"))createDoc("text/csv","noteit-notebooks.csv",REQ_EXPORT_BOOKS_CSV); else if(s.equals("import_all")||s.equals("import_notes")||s.equals("import_books")){ Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.setType("*/*"); i.addCategory(Intent.CATEGORY_OPENABLE); startActivityForResult(i,s.equals("import_notes")?REQ_IMPORT_NOTES:s.equals("import_books")?REQ_IMPORT_BOOKS:REQ_IMPORT_ALL); } else if(s.contains("cache")){ clearDir(getCacheDir()); toast("Cache cleared"); } else if(s.contains("Reset")){ new AlertDialog.Builder(this).setTitle("Reset Noteit?").setMessage("This deletes all local notebooks and notes.").setPositiveButton("Reset",(d,w)->{notebooks.clear();notes.clear();ensureDefault();save();showHome(0);}).setNegativeButton("Cancel",null).show(); } else showAppInfo(); }
    void createDoc(String type,String title,int req){ Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT); i.setType(type); i.putExtra(Intent.EXTRA_TITLE,title); startActivityForResult(i,req); }
    void showAppInfo(){ new AlertDialog.Builder(this).setTitle("Noteit").setMessage("Version "+APP_VERSION+"\n\nGitHub Repository:\n"+GITHUB_URL).setPositiveButton("OK",null).show(); }

    public void onBackPressed(){ if(editor!=null&&current!=null){commit();showHome(0);} else showHome(0); }
    protected void onActivityResult(int r,int c,Intent data){ super.onActivityResult(r,c,data); if(c!=RESULT_OK||data==null)return; try{ Uri u=data.getData(); if(r==REQ_EXPORT_NOTE_ONE)writeText(u,toMarkdown(current)); else if(r==REQ_IMAGE)insert("![image]("+u.toString()+")"); else if(r==REQ_EXPORT_ALL)writeZip(u,true,true); else if(r==REQ_EXPORT_NOTES_ZIP)writeZip(u,true,false); else if(r==REQ_EXPORT_BOOKS_ZIP)writeZip(u,false,true); else if(r==REQ_EXPORT_NOTES_MD)writeText(u,exportMarkdownAll()); else if(r==REQ_EXPORT_NOTES_TXT)writeText(u,exportTextAll()); else if(r==REQ_EXPORT_NOTES_CSV)writeText(u,exportNotesCsv()); else if(r==REQ_EXPORT_BOOKS_CSV)writeText(u,exportBooksCsv()); else if(r==REQ_IMPORT_ALL||r==REQ_IMPORT_NOTES||r==REQ_IMPORT_BOOKS){importFile(u,r);showHome(0);} }catch(Exception ex){toast("Error: "+ex.getMessage());} }

    void load(){ notes.clear(); notebooks.clear(); File old=new File(getFilesDir(),"notes.json"); try{ if(!store.exists()&&old.exists()) store=old; if(!store.exists()){ensureDefault();return;} String raw=readFile(store); if(raw.trim().startsWith("[")){ JSONArray a=new JSONArray(raw); ensureDefault(); importLegacyArray(a); return; } JSONObject root=new JSONObject(raw); JSONArray bs=root.optJSONArray("notebooks"); if(bs!=null) for(int i=0;i<bs.length();i++){ JSONObject o=bs.getJSONObject(i); Notebook b=new Notebook(); b.id=o.optString("id",UUID.randomUUID().toString()); b.title=o.optString("title","Personal"); b.created=o.optLong("created",System.currentTimeMillis()); b.updated=o.optLong("updated",b.created); b.color=o.optInt("color",colorForIndex(notebooks.size())); notebooks.add(b); } ensureDefault(); JSONArray ns=root.optJSONArray("notes"); if(ns!=null) for(int i=0;i<ns.length();i++){ JSONObject o=ns.getJSONObject(i); Note n=new Note(); n.id=o.optString("id",UUID.randomUUID().toString()); n.notebookId=o.optString("notebookId",selectedNotebookId); n.title=o.optString("title",""); n.body=o.optString("body",""); n.created=o.optLong("created",System.currentTimeMillis()); n.updated=o.optLong("updated",n.created); notes.add(n); } selectedNotebookId=root.optString("selectedNotebookId",notebooks.get(0).id); }catch(Exception e){ensureDefault();toast("Load failed");} }
    void ensureDefault(){ if(notebooks.size()==0){ Notebook b=new Notebook(); b.id="default"; b.title="Personal"; b.color=colorForIndex(0); notebooks.add(b); } for(int i=0;i<notebooks.size();i++) if(notebooks.get(i).color==0) notebooks.get(i).color=colorForIndex(i); selectedNotebookId=notebooks.get(0).id; }
    void importLegacyArray(JSONArray a)throws Exception{ for(int i=0;i<a.length();i++){ JSONObject o=a.getJSONObject(i); Note n=new Note(); n.notebookId=selectedNotebookId; n.title=o.optString("title",""); n.body=o.optString("body",""); n.created=o.optLong("created",System.currentTimeMillis()); n.updated=o.optLong("updated",n.created); notes.add(n);} save(); }
    void save(){ try{ JSONObject out=new JSONObject(); out.put("version",APP_VERSION); out.put("selectedNotebookId",selectedNotebookId); JSONArray bs=new JSONArray(); for(Notebook b:notebooks){ JSONObject o=new JSONObject(); o.put("id",b.id); o.put("title",b.title); o.put("created",b.created); o.put("updated",b.updated); o.put("color",b.color); bs.put(o);} JSONArray ns=new JSONArray(); for(Note n:notes){ JSONObject o=new JSONObject(); o.put("id",n.id); o.put("notebookId",n.notebookId); o.put("title",n.title); o.put("body",n.body); o.put("created",n.created); o.put("updated",n.updated); ns.put(o);} out.put("notebooks",bs); out.put("notes",ns); try(FileOutputStream f=new FileOutputStream(new File(getFilesDir(),"noteit-store.json"))){f.write(out.toString(2).getBytes("UTF-8"));} }catch(Exception e){toast("Save failed");} }

    void writeZip(Uri u, boolean includeNotes, boolean includeBooks)throws Exception{ ZipOutputStream z=new ZipOutputStream(getContentResolver().openOutputStream(u)); if(includeBooks) zipEntry(z,"notebooks.csv",exportBooksCsv()); if(includeNotes){ zipEntry(z,"notes.csv",exportNotesCsv()); zipEntry(z,"notes.md",exportMarkdownAll()); zipEntry(z,"notes.txt",exportTextAll()); for(Note n:notes) zipEntry(z,"notes/"+safe(n.title.length()==0?"untitled-note":n.title)+".md",toMarkdown(n)); } if(includeNotes&&includeBooks) zipEntry(z,"noteit-store.json",exportJson()); z.close(); toast("Export completed"); }
    void zipEntry(ZipOutputStream z,String name,String text)throws Exception{ z.putNextEntry(new ZipEntry(name)); z.write(text.getBytes("UTF-8")); z.closeEntry(); }
    String exportJson()throws Exception{ JSONObject out=new JSONObject(readFile(new File(getFilesDir(),"noteit-store.json"))); return out.toString(2); }
    String exportMarkdownAll(){ StringBuilder b=new StringBuilder(); for(Note n:notes)b.append("# ").append(n.title.length()==0?"Untitled note":n.title).append("\n\n").append(n.body).append("\n\n---\n\n"); return b.toString(); }
    String exportTextAll(){ StringBuilder b=new StringBuilder(); for(Note n:notes)b.append(n.title.length()==0?"Untitled note":n.title).append("\n").append(bookTitle(n.notebookId)).append("\n").append(fmt(n.updated)).append("\n\n").append(n.body).append("\n\n---\n\n"); return b.toString(); }
    String exportNotesCsv(){ StringBuilder csv=new StringBuilder("notebook,title,updated,body\n"); for(Note n:notes)csv.append(csvCell(bookTitle(n.notebookId))).append(',').append(csvCell(n.title)).append(',').append(n.updated).append(',').append(csvCell(n.body)).append("\n"); return csv.toString(); }
    String exportBooksCsv(){ StringBuilder csv=new StringBuilder("title,updated,noteCount\n"); for(Notebook b:notebooks)csv.append(csvCell(b.title)).append(',').append(b.updated).append(',').append(countNotes(b.id)).append("\n"); return csv.toString(); }
    String toMarkdown(Note n){ return "# "+(n.title.length()==0?"Untitled note":n.title)+"\n\n"+n.body+"\n"; }
    void writeText(Uri u,String s)throws Exception{ try(OutputStream os=getContentResolver().openOutputStream(u)){os.write(s.getBytes("UTF-8"));} toast("Export completed"); }

    void importFile(Uri u,int mode)throws Exception{
        InputStream probe=getContentResolver().openInputStream(u); byte[] sig=new byte[2]; int n=probe.read(sig); probe.close();
        if(n==2 && sig[0]=='P' && sig[1]=='K'){ importZip(u,mode); save(); toast("Import completed"); return; }
        String s=readUri(u);
        if(s.trim().startsWith("{")){ JSONObject o=new JSONObject(s); importStoreObject(o,mode); }
        else if(s.toLowerCase(Locale.US).contains("title,updated,notecount")){ importBooksCsv(s); }
        else if(s.toLowerCase(Locale.US).startsWith("notebook,title,updated,body")){ importNotesCsv(s); }
        else { Note note=new Note(); note.notebookId=selectedNotebookId; note.title="Imported note"; note.body=s; notes.add(0,note); }
        save(); toast("Import completed");
    }
    void importZip(Uri u,int mode)throws Exception{
        ZipInputStream zin=new ZipInputStream(getContentResolver().openInputStream(u)); ZipEntry e;
        while((e=zin.getNextEntry())!=null){
            if(e.isDirectory()) continue;
            ByteArrayOutputStream b=new ByteArrayOutputStream(); byte[] buf=new byte[8192]; int r; while((r=zin.read(buf))>0)b.write(buf,0,r);
            String name=e.getName().toLowerCase(Locale.US); String text=b.toString("UTF-8");
            if(name.endsWith("noteit-store.json")){ importStoreObject(new JSONObject(text),mode); }
            else if(mode!=REQ_IMPORT_BOOKS && name.endsWith("notes.csv")){ importNotesCsv(text); }
            else if(mode!=REQ_IMPORT_NOTES && name.endsWith("notebooks.csv")){ importBooksCsv(text); }
            else if(mode!=REQ_IMPORT_BOOKS && (name.endsWith(".md") || name.endsWith(".txt")) && name.startsWith("notes/")){ Note note=new Note(); note.notebookId=selectedNotebookId; note.title=e.getName().substring(e.getName().lastIndexOf('/')+1).replaceAll("\\.(md|txt)$",""); note.body=text; notes.add(0,note); }
        }
        zin.close();
    }
    void importStoreObject(JSONObject o,int mode)throws Exception{
        if(mode!=REQ_IMPORT_NOTES){ JSONArray bs=o.optJSONArray("notebooks"); if(bs!=null) for(int i=0;i<bs.length();i++) importBookObj(bs.getJSONObject(i)); }
        if(mode!=REQ_IMPORT_BOOKS){ JSONArray ns=o.optJSONArray("notes"); if(ns!=null) for(int i=0;i<ns.length();i++) importNoteObj(ns.getJSONObject(i)); }
    }
    void importNoteObj(JSONObject o){ Note n=new Note(); n.id=o.optString("id",UUID.randomUUID().toString()); n.notebookId=o.optString("notebookId",selectedNotebookId); n.title=o.optString("title",""); n.body=o.optString("body",""); n.created=o.optLong("created",System.currentTimeMillis()); n.updated=System.currentTimeMillis(); notes.add(0,n); }
    void importBookObj(JSONObject o){ Notebook b=new Notebook(); b.id=o.optString("id",UUID.randomUUID().toString()); b.title=o.optString("title","Imported notebook"); b.created=o.optLong("created",System.currentTimeMillis()); b.updated=System.currentTimeMillis(); b.color=o.optInt("color",nextNotebookColor()); notebooks.add(0,b); }
    void importBooksCsv(String s){ String[] lines=s.split("\n"); for(int i=1;i<lines.length;i++){ ArrayList<String> c=parseCsvLine(lines[i]); if(c.size()>0&&c.get(0).trim().length()>0){ Notebook b=new Notebook(); b.title=c.get(0).trim(); b.color=nextNotebookColor(); notebooks.add(0,b); } } }
    void importNotesCsv(String s){ String[] lines=s.split("\n"); for(int i=1;i<lines.length;i++){ ArrayList<String> c=parseCsvLine(lines[i]); if(c.size()>=4){ String bid=findOrCreateBook(c.get(0)); Note n=new Note(); n.notebookId=bid; n.title=c.get(1); n.body=c.get(3); try{n.updated=Long.parseLong(c.get(2));}catch(Exception ignored){} notes.add(0,n); } } }
    String findOrCreateBook(String title){ for(Notebook b:notebooks) if(b.title.equals(title)) return b.id; Notebook b=new Notebook(); b.title=title.length()==0?"Imported":title; b.color=nextNotebookColor(); notebooks.add(0,b); return b.id; }
    ArrayList<String> parseCsvLine(String line){ ArrayList<String> out=new ArrayList<>(); StringBuilder cur=new StringBuilder(); boolean q=false; for(int i=0;i<line.length();i++){ char ch=line.charAt(i); if(ch=='\"'){ if(q&&i+1<line.length()&&line.charAt(i+1)=='\"'){cur.append('"');i++;} else q=!q; } else if(ch==','&&!q){out.add(cur.toString());cur.setLength(0);} else cur.append(ch);} out.add(cur.toString()); return out; }

    void chooseNotebook(TextView view){ String[] names=new String[notebooks.size()]; int checked=0; for(int i=0;i<notebooks.size();i++){names[i]=notebooks.get(i).title; if(notebooks.get(i).id.equals(current.notebookId))checked=i;} new AlertDialog.Builder(this).setTitle("Move to notebook").setSingleChoiceItems(names,checked,(d,which)->{ current.notebookId=notebooks.get(which).id; selectedNotebookId=current.notebookId; commit(); view.setText("Notebook: "+bookTitle(current.notebookId)); view.setBackground(glassStroke(Color.argb(105,255,255,255),dp(24),accentForBook(current.notebookId))); d.dismiss(); }).setNegativeButton("Cancel",null).show(); }
    Notebook bookById(String id){ for(Notebook b:notebooks) if(b.id.equals(id)) return b; return notebooks.size()>0?notebooks.get(0):null; }
    int accentForBook(String id){ Notebook b=bookById(id); return b==null?PURPLE:b.color; }
    int colorForIndex(int i){ int[] palette={Color.rgb(132,69,255),Color.rgb(56,189,248),Color.rgb(34,197,94),Color.rgb(245,158,11),Color.rgb(236,72,153),Color.rgb(99,102,241),Color.rgb(20,184,166),Color.rgb(248,113,113)}; return palette[Math.abs(i)%palette.length]; }
    int nextNotebookColor(){ return colorForIndex(notebooks.size()); }
    String fallbackNotebookId(String excluded){ for(Notebook b:notebooks) if(!b.id.equals(excluded)) return b.id; return excluded; }
    GradientDrawable glassStroke(int color,int radius,int strokeColor){ GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{color,Color.argb(56,255,255,255)}); g.setCornerRadius(radius); g.setStroke(dp(2),Color.argb(220,Color.red(strokeColor),Color.green(strokeColor),Color.blue(strokeColor))); return g; }
    Notebook selectedBook(){ for(Notebook b:notebooks) if(b.id.equals(selectedNotebookId)) return b; return notebooks.get(0); }
    String bookTitle(String id){ for(Notebook b:notebooks) if(b.id.equals(id)) return b.title; return "Personal"; }
    int countNotes(String id){ int c=0; for(Note n:notes) if(n.notebookId.equals(id)) c++; return c; }
    String csvCell(String s){ return "\""+(s==null?"":s).replace("\"","\"\"")+"\""; }
    String safe(String s){ return s.replaceAll("[^a-zA-Z0-9._-]","_"); }
    String preview(String s){ s=s.replace('\n',' ').trim(); return s.length()>120?s.substring(0,120)+"…":(s.length()==0?"No content yet":s); }
    String fmt(long t){ return new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.US).format(new Date(t)); }
    String readUri(Uri u)throws Exception{ InputStream in=getContentResolver().openInputStream(u); ByteArrayOutputStream b=new ByteArrayOutputStream(); byte[] buf=new byte[8192]; int n; while((n=in.read(buf))>0)b.write(buf,0,n); return b.toString("UTF-8"); }
    String readFile(File f)throws Exception{ return new String(java.nio.file.Files.readAllBytes(f.toPath()),"UTF-8"); }
    void clearDir(File f){ if(f==null||!f.exists())return; File[] fs=f.listFiles(); if(fs!=null)for(File x:fs){ if(x.isDirectory())clearDir(x); x.delete(); } }
    int dp(int v){ return (int)(v*getResources().getDisplayMetrics().density+.5f); }
    void toast(String s){ Toast.makeText(this,s,Toast.LENGTH_SHORT).show(); }

    class GlowBackground extends Drawable { Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); public void draw(Canvas c){ Rect b=getBounds(); c.drawColor(BG); p.setShader(new RadialGradient(b.width()*.18f,b.height()*.08f,b.width()*.72f,Color.argb(110,132,69,255),Color.TRANSPARENT,Shader.TileMode.CLAMP)); c.drawCircle(b.width()*.18f,b.height()*.08f,b.width()*.72f,p); p.setShader(new RadialGradient(b.width()*.92f,b.height()*.34f,b.width()*.55f,Color.argb(82,178,85,255),Color.TRANSPARENT,Shader.TileMode.CLAMP)); c.drawCircle(b.width()*.92f,b.height()*.34f,b.width()*.55f,p); p.setShader(null);} public void setAlpha(int a){} public void setColorFilter(android.graphics.ColorFilter cf){} public int getOpacity(){return PixelFormat.OPAQUE;} }
    static class Note { String id=UUID.randomUUID().toString(), notebookId="default", title="", body=""; long created=System.currentTimeMillis(), updated=created; }
    static class Notebook { String id=UUID.randomUUID().toString(), title="Personal"; int color=0; long created=System.currentTimeMillis(), updated=created; }
}
