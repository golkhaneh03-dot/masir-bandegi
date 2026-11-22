package ir.masirbandegi.app;

import android.content.*;
import android.database.*;
import android.database.sqlite.*;
import java.time.LocalDate;
import java.util.*;

public class Store extends SQLiteOpenHelper {
  public Store(Context c){super(c,"masir_bandegi.db",null,1);}
  @Override public void onCreate(SQLiteDatabase db){
    db.execSQL("CREATE TABLE tasks(id INTEGER PRIMARY KEY AUTOINCREMENT, day TEXT NOT NULL, title TEXT NOT NULL, category TEXT, status INTEGER NOT NULL DEFAULT 0, note TEXT, done_at TEXT)");
    db.execSQL("CREATE TABLE reflections(day TEXT PRIMARY KEY, answers TEXT NOT NULL)");
    db.execSQL("CREATE TABLE learning(id INTEGER PRIMARY KEY AUTOINCREMENT, day TEXT, skill TEXT, topic TEXT, minutes INTEGER, progress INTEGER, note TEXT)");
    db.execSQL("CREATE TABLE settings(k TEXT PRIMARY KEY,v TEXT)");
  }
  @Override public void onUpgrade(SQLiteDatabase db,int a,int b){}
  public void ensureDay(String day){
    SQLiteDatabase db=getWritableDatabase(); Cursor c=db.rawQuery("SELECT COUNT(*) FROM tasks WHERE day=?",new String[]{day}); c.moveToFirst(); int n=c.getInt(0); c.close(); if(n>0)return;
    String[][] items={
      {"نماز صبح","عبادت"},{"نماز ظهر","عبادت"},{"نماز عصر","عبادت"},{"نماز مغرب","عبادت"},{"نماز عشا","عبادت"},
      {"یاد امام زمان (عج) پس از نمازها","عبادت"},{"۱۰۰ مرتبه استغفار","ذکر"},{"۱۰۰ صلوات هدیه به امام زمان (عج)","ذکر"},
      {"۱۰۰ صلوات هدیه به حضرت زهرا (س)","ذکر"},{"یک صفحه قرآن همراه تفسیر","قرآن"},{"مطالعه کوتاه روزانه","رشد"},
      {"خلوت شبانه با خدا","عبادت"},{"زیارت عاشورا","زیارت"},{"انتخاب دعا: آل‌یاسین، کمیل یا علقمه","دعا"},
      {"نماز استغفار پس از مغرب و عشا","عبادت"},{"مراقبت از چشم، فکر و زبان","اخلاق"},
      {"فعالیت شغلی و شخصی","کار"},{"تمرین امنیت شبکه","مهارت"},{"تمرین زبان عبری","مهارت"},{"تمرین OSINT","مهارت"},{"تمرین هوش مصنوعی","مهارت"},{"تمرین عکاسی","مهارت"}
    };
    for(String[] x:items){ContentValues v=new ContentValues();v.put("day",day);v.put("title",x[0]);v.put("category",x[1]);db.insert("tasks",null,v);}
  }
  public Cursor tasks(String day){return getReadableDatabase().rawQuery("SELECT id,title,category,status,note FROM tasks WHERE day=? ORDER BY id",new String[]{day});}
  public void status(long id,int status){ContentValues v=new ContentValues();v.put("status",status);v.put("done_at",status==0?null:java.time.LocalTime.now().toString());getWritableDatabase().update("tasks",v,"id=?",new String[]{String.valueOf(id)});}
  public void addTask(String day,String title){ContentValues v=new ContentValues();v.put("day",day);v.put("title",title);v.put("category","شخصی");getWritableDatabase().insert("tasks",null,v);}
  public void saveReflection(String day,String text){ContentValues v=new ContentValues();v.put("day",day);v.put("answers",text);getWritableDatabase().insertWithOnConflict("reflections",null,v,SQLiteDatabase.CONFLICT_REPLACE);}
  public String reflection(String day){Cursor c=getReadableDatabase().rawQuery("SELECT answers FROM reflections WHERE day=?",new String[]{day});String s="";if(c.moveToFirst())s=c.getString(0);c.close();return s;}
  public void addLearning(String day,String skill,String topic,int mins,int progress,String note){ContentValues v=new ContentValues();v.put("day",day);v.put("skill",skill);v.put("topic",topic);v.put("minutes",mins);v.put("progress",progress);v.put("note",note);getWritableDatabase().insert("learning",null,v);}
  public int[] counts(String day){Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*),SUM(CASE WHEN status=1 THEN 1 ELSE 0 END),SUM(CASE WHEN status=2 THEN 1 ELSE 0 END) FROM tasks WHERE day=?",new String[]{day});c.moveToFirst();int[] a={c.getInt(0),c.isNull(1)?0:c.getInt(1),c.isNull(2)?0:c.getInt(2)};c.close();return a;}
}
