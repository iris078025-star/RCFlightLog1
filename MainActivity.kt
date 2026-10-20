package com.rcflightlog

import android.app.*
import android.os.*
import android.content.*
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.Color
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.*
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : Activity() {
 private lateinit var db: SQLiteDatabase
 private lateinit var root: LinearLayout
 private var planeIndex = 0
 private var pendingPhotoPlane = -1L
 private val photoRequest = 100
 private val exportRequest = 101
 private val importRequest = 102
 private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
 private val timestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
 private val navy = Color.rgb(16,28,47)
 private val teal = Color.rgb(0,135,130)
 private val planes get() = query("SELECT id,name,photo FROM planes ORDER BY id")
 override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState)
  window.statusBarColor=navy; window.navigationBarColor=navy
  db = object:SQLiteOpenHelper(this,"rcflightlog.db",null,1){
   override fun onCreate(d:SQLiteDatabase){
    d.execSQL("CREATE TABLE planes(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,photo TEXT DEFAULT '')")
    d.execSQL("CREATE TABLE batteries(id INTEGER PRIMARY KEY AUTOINCREMENT,plane_id INTEGER NOT NULL,name TEXT NOT NULL,capacity INTEGER NOT NULL DEFAULT 0,initial_cycles INTEGER NOT NULL DEFAULT 0)")
    d.execSQL("CREATE TABLE flights(id INTEGER PRIMARY KEY AUTOINCREMENT,battery_id INTEGER NOT NULL,flown_at TEXT NOT NULL,duration_sec INTEGER NOT NULL DEFAULT 0)")
   }
   override fun onUpgrade(d:SQLiteDatabase,old:Int,new:Int){}
  }.writableDatabase
  if (planes.isEmpty()) db.execSQL("INSERT INTO planes(name) VALUES ('M1 V3 Pro')")
  render()
 }
 private fun query(sql:String):List<JSONObject>{ val list= mutableListOf<JSONObject>(); val c=db.rawQuery(sql,null); c.use { while(it.moveToNext()){val o=JSONObject();for(i in 0 until it.columnCount)o.put(it.getColumnName(i),if(it.isNull(i)) JSONObject.NULL else it.getString(i));list.add(o)} };return list }
 private fun esc(s:String)=s.replace("'","''")
 private fun id(o:JSONObject)=o.optLong("id")
 private fun label(s:String,size:Float=17f,bold:Boolean=false):TextView=TextView(this).apply {text=s;textSize=size;setTextColor(navy);setPadding(8,9,8,9);if(bold)setTypeface(null,1)}
 private fun button(s:String,action:()->Unit)=Button(this).apply {text=s;setOnClickListener{action()}}
 private fun column()=LinearLayout(this).apply {orientation=LinearLayout.VERTICAL;setPadding(16,10,16,10)}
 private fun row()=LinearLayout(this).apply {orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
 private fun input(hint:String,value:String="",number:Boolean=false)=EditText(this).apply {this.hint=hint;setText(value);if(number)inputType=android.text.InputType.TYPE_CLASS_NUMBER}
 private fun alert(title:String,content:View,positive:String="儲存",save:()->Unit){AlertDialog.Builder(this).setTitle(title).setView(content).setPositiveButton(positive){_,_->save()}.setNegativeButton("取消",null).show()}
 private fun current():JSONObject?=planes.getOrNull(planeIndex.coerceIn(0,(planes.size-1).coerceAtLeast(0)))
 private fun render(){
  val ps=planes;planeIndex=planeIndex.coerceIn(0,(ps.size-1).coerceAtLeast(0))
  root=column();root.setBackgroundColor(Color.rgb(245,248,250));setContentView(root)
  val header=row();header.addView(label("RC FLIGHT LOG",23f,true),LinearLayout.LayoutParams(0,-2,1f));header.addView(button("＋ 新增飛機"){editPlane(null)});root.addView(header)
  val nav=row();nav.addView(button("❮"){if(planeIndex>0){planeIndex--;render()}})
  nav.addView(label("${planeIndex+1} / ${ps.size}",15f),LinearLayout.LayoutParams(0,-2,1f));nav.addView(button("❯"){if(planeIndex<ps.lastIndex){planeIndex++;render()}});root.addView(nav)
  val p=current() ?:return;val pid=id(p)
  val content=ScrollView(this);root.addView(content,LinearLayout.LayoutParams(-1,0,1f));val body=column();content.addView(body)
  val ph=p.optString("photo");if(ph.isNotEmpty())try { val stream=contentResolver.openInputStream(Uri.parse(ph));val bitmap=BitmapFactory.decodeStream(stream);stream?.close();if(bitmap!=null){val iv=ImageView(this);iv.setImageBitmap(bitmap);iv.scaleType=ImageView.ScaleType.CENTER_CROP;body.addView(iv,LinearLayout.LayoutParams(-1,220))} }catch(_:Exception){}
  body.addView(label(p.optString("name"),27f,true))
  val actions=row();actions.addView(button("編輯飛機"){editPlane(p)});actions.addView(button("選擇照片"){pendingPhotoPlane=pid;startActivityForResult(Intent(Intent.ACTION_GET_CONTENT).apply{type="image/*"},photoRequest)});actions.addView(button("新增電池"){editBattery(pid,null)});body.addView(actions)
  val batteries=query("SELECT * FROM batteries WHERE plane_id=$pid ORDER BY id")
  val today=dateFormat.format(Date());val used=batteries.count{query("SELECT id FROM flights WHERE battery_id=${id(it)} AND substr(flown_at,1,10)='$today'").isNotEmpty()}
  body.addView(label("今日已使用 $used / ${batteries.size} 顆電池",19f,true))
  for(b in batteries){val bid=id(b);val flights=query("SELECT * FROM flights WHERE battery_id=$bid ORDER BY flown_at DESC,id DESC");val count=b.optInt("initial_cycles")+flights.size
   val todayFlights=flights.filter{it.optString("flown_at").startsWith(today)};val duration=todayFlights.sumOf{it.optInt("duration_sec")}
   val card=column();card.setBackgroundColor(Color.WHITE);val line=row();line.addView(label("${b.optString("name")}  ·  ${b.optInt("capacity")} mAh",19f,true),LinearLayout.LayoutParams(0,-2,1f));line.addView(label(if(todayFlights.isEmpty())"未使用" else "今日已使用",14f));card.addView(line)
   card.addView(label("累積 $count 次使用  |  今日 ${todayFlights.size} 趟  |  今日 ${duration/60}分${duration%60}秒",15f))
   val controls=row();controls.addView(button("＋ 記錄飛行"){addFlight(bid)});controls.addView(button("歷史"){history(b)});controls.addView(button("編輯"){editBattery(pid,b)});card.addView(controls);body.addView(card)
   body.addView(label("",5f))
  }
  val footer=row();footer.addView(button("匯出備份"){startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply{addCategory(Intent.CATEGORY_OPENABLE);type="application/json";putExtra(Intent.EXTRA_TITLE,"rcflightlog-backup.json")},exportRequest)});footer.addView(button("匯入備份"){startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{addCategory(Intent.CATEGORY_OPENABLE);type="application/json"},importRequest)});root.addView(footer)
  root.setOnTouchListener(object:View.OnTouchListener{var x=0f;override fun onTouch(v:View,e:android.view.MotionEvent):Boolean{if(e.action==0){x=e.x;return true};if(e.action==1){val delta=e.x-x;if(kotlin.math.abs(delta)>90){planeIndex=(planeIndex+if(delta<0)1 else -1).coerceIn(0,ps.lastIndex);render()};return true};return true}})
  content.setOnTouchListener(object:View.OnTouchListener{var x=0f;var y=0f;override fun onTouch(v:View,e:android.view.MotionEvent):Boolean{if(e.action==0){x=e.x;y=e.y;return false};if(e.action==1&&kotlin.math.abs(e.x-x)>110&&kotlin.math.abs(e.x-x)>kotlin.math.abs(e.y-y)*1.4){planeIndex=(planeIndex+if(e.x<x)1 else -1).coerceIn(0,ps.lastIndex);render();return true};return false}})
 }
 private fun editPlane(p:JSONObject?){val layout=column();val name=input("飛機名稱",p?.optString("name")?:"");layout.addView(name)
  if(p!=null)layout.addView(button("刪除飛機與所有電池紀錄"){AlertDialog.Builder(this).setMessage("確定刪除此飛機及所有電池、飛行紀錄？").setPositiveButton("刪除"){_,_->val pid=id(p);db.execSQL("DELETE FROM flights WHERE battery_id IN (SELECT id FROM batteries WHERE plane_id=$pid)");db.execSQL("DELETE FROM batteries WHERE plane_id=$pid");db.execSQL("DELETE FROM planes WHERE id=$pid");planeIndex=0;render()}.setNegativeButton("取消",null).show()})
  alert(if(p==null)"新增飛機" else "編輯飛機",layout){val n=name.text.toString().trim();if(n.isNotEmpty()){if(p==null){db.execSQL("INSERT INTO planes(name) VALUES ('${esc(n)}')");planeIndex=planes.lastIndex}else db.execSQL("UPDATE planes SET name='${esc(n)}' WHERE id=${id(p)}");render()}}
 }
 private fun editBattery(pid:Long,b:JSONObject?){val layout=column();val name=input("電池編號，例如 BAT-01",b?.optString("name")?:"");val cap=input("容量 mAh",b?.optString("capacity")?:"",true);val cycles=input("已有使用次數",b?.optString("initial_cycles")?:"0",true);layout.addView(name);layout.addView(cap);layout.addView(cycles)
  if(b!=null)layout.addView(button("刪除電池及飛行紀錄"){AlertDialog.Builder(this).setMessage("確定永久刪除這顆電池及所有紀錄？").setPositiveButton("刪除"){_,_->db.execSQL("DELETE FROM flights WHERE battery_id=${id(b)}");db.execSQL("DELETE FROM batteries WHERE id=${id(b)}");render()}.setNegativeButton("取消",null).show()})
  alert(if(b==null)"新增電池" else "編輯電池",layout){val n=name.text.toString().trim();if(n.isNotEmpty()){val c=cap.text.toString().toIntOrNull()?.coerceAtLeast(0)?:0;val cy=cycles.text.toString().toIntOrNull()?.coerceAtLeast(0)?:0;if(b==null)db.execSQL("INSERT INTO batteries(plane_id,name,capacity,initial_cycles) VALUES ($pid,'${esc(n)}',$c,$cy)")else db.execSQL("UPDATE batteries SET name='${esc(n)}',capacity=$c,initial_cycles=$cy WHERE id=${id(b)}");render()}}
 }
 private fun addFlight(bid:Long){val layout=column();val min=input("飛行分鐘","5",true);val sec=input("飛行秒數","0",true);layout.addView(label("完成飛行後記錄；時間預設為現在"));layout.addView(min);layout.addView(sec)
  alert("新增飛行紀錄",layout,"完成記錄"){val m=min.text.toString().toIntOrNull()?:0;val s=sec.text.toString().toIntOrNull()?:0;if(m>=0&&s in 0..59){val now=timestampFormat.format(Date());db.execSQL("INSERT INTO flights(battery_id,flown_at,duration_sec) VALUES ($bid,'$now',${m*60+s})");render()}}
 }
 private fun history(b:JSONObject){val bid=id(b);val flights=query("SELECT * FROM flights WHERE battery_id=$bid ORDER BY flown_at DESC,id DESC");val layout=column();layout.addView(label("共 ${flights.size} 筆飛行紀錄（不含初始次數）"));for(f in flights){val line=row();val sec=f.optInt("duration_sec");line.addView(label("${f.optString("flown_at")}  ${sec/60}分${sec%60}秒",14f),LinearLayout.LayoutParams(0,-2,1f));line.addView(button("刪除"){AlertDialog.Builder(this).setMessage("刪除此筆飛行紀錄？").setPositiveButton("刪除"){_,_->db.execSQL("DELETE FROM flights WHERE id=${id(f)}");history(b);render()}.setNegativeButton("取消",null).show()});layout.addView(line)}
  val scroll=ScrollView(this);scroll.addView(layout);AlertDialog.Builder(this).setTitle(b.optString("name")+" · 歷史").setView(scroll).setPositiveButton("關閉",null).show()
 }
 private fun backup():String {val result=JSONObject();result.put("format",1);for(t in listOf("planes","batteries","flights")){val a=JSONArray();query("SELECT * FROM $t ORDER BY id").forEach{a.put(it)};result.put(t,a)};return result.toString(2)}
 private fun restore(data:String){val obj=JSONObject(data);require(obj.getInt("format")==1);val names=listOf("planes","batteries","flights");for(n in names)require(obj.getJSONArray(n).length()>=0)
  db.beginTransaction();try{for(t in names.reversed())db.execSQL("DELETE FROM $t");for(t in names){val arr=obj.getJSONArray(t);for(i in 0 until arr.length()){val o=arr.getJSONObject(i);val keys=o.keys().asSequence().toList();val values=keys.map{ k -> if(o.isNull(k))"NULL" else "'${esc(o.getString(k))}'" };db.execSQL("INSERT INTO $t (${keys.joinToString(",")}) VALUES (${values.joinToString(",")})")}};db.setTransactionSuccessful()}finally{db.endTransaction()};planeIndex=0;render()
 }
 @Deprecated("Deprecated in Android") override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){super.onActivityResult(requestCode,resultCode,data);if(resultCode!=RESULT_OK)return;val uri=data?.data?:return
  try{when(requestCode){photoRequest->{val bytes=contentResolver.openInputStream(uri)?.use{it.readBytes()}?:return;val f=java.io.File(filesDir,"plane_$pendingPhotoPlane.jpg");f.writeBytes(bytes);db.execSQL("UPDATE planes SET photo='${esc(Uri.fromFile(f).toString())}' WHERE id=$pendingPhotoPlane");render()}
   exportRequest->{contentResolver.openOutputStream(uri)?.bufferedWriter()?.use{it.write(backup())};Toast.makeText(this,"備份已匯出",Toast.LENGTH_SHORT).show()}
   importRequest->{val json=contentResolver.openInputStream(uri)?.bufferedReader()?.use{it.readText()}?:return;AlertDialog.Builder(this).setMessage("匯入會覆蓋現有全部資料，確定繼續？").setPositiveButton("覆蓋匯入"){_,_->try{restore(json)}catch(e:Exception){Toast.makeText(this,"匯入失敗：${e.message}",Toast.LENGTH_LONG).show()}}.setNegativeButton("取消",null).show()}
  }}catch(e:Exception){Toast.makeText(this,"操作失敗：${e.message}",Toast.LENGTH_LONG).show()}
 }
}
