# FCM Push
___
##최종목표
-
`원래목표 :     
` 
<br>
~~aws mobilehub와 pinpoint를 이용해 push알림 구현하기 **_180720-180723**~~
<br><br>
`수정된목표 : 
`   
- [x] 안드로이드스튜디오와 node.js를 이용해 웹에서 입력해서 푸시알림받기 **_180730**
- [ ] ~~토큰을 mysql 데이터베이스에 저장하기~~   
     ~~->**180729** DB에 랜덤으로 저장된다....~~
- [x] 푸시알림 허용여부에 따라 알림보내기  **_180731**
- [x] 푸시알림창에 이미지 보내기 **_180804**
- [x] 오레오에서 진동모드일때만 진동오는 거 해결하기 **_180806**
- [x] fcm php node.js로 옮겨기 **_180807**
- [x] html페이지 ajax로 비동기처리하기 **_180808**
- [x] 푸시왔을시 화면깨우기 **_181120**
- [x] 광고알림(이미지+텍스트)과 주문상태알림(텍스트만) 구분해서 안드로이드단에서 받기 **_181120**

`나중으로 미룬 목표 : 
`   
- [ ] ~~html 파일열기로 이미지 파일 s3로 올린후 url값 가져와서 fcm보내기~~

`애로사항 : 
`   
~~~
안드로이드스튜디오, php, 코틀린을 전혀 접해보지 않은 터라 여러가지 오류에 대처하는 것과 설정하는 것이 힘들었다.
firebase fcm설명서와 거의 대부분의 구글자료들이 gcm을 기반으로 하고 있거나 
18년 7월초에 바뀐 함수를 제대로 써놓은 곳이 없어(onTokenRefresh->onNewToken 등 deprecated된 함수들이 많다.)
안드로이드스튜디오와 firebase를 처음 접한 나에게는 이해하고 적용하기가 어려웠다.
대부분의 웹서버 연결은 스프링프로젝트나 php가 많아 php로 해보고 나중에 옮겨보려고 한다.
~~~
___
#180723-26 aws pinpoint -> firebase fcm
-
~~~
aws pinpoint 자체가 mobilehub에 종속적이었고,
pinpoint시스템에서 사용하려는 서버키를 받으려면 firebase프로젝트를 만들어 서버키를 만들어야 했다.
또한 url을 보내거나 푸시를 클릭했을 때 연결하는 방식, 
며칠, 몇 달 전에 앱에 접근했던 사람에게 푸시알림을 전달하는 방식을 마음에 들었지만,
사용자를 그룹지어 메세지를 보내거나, 알림 허용여부에 따른 푸시를 구현하는 것이 거의 불가능해 보였다. 
게다가 제일 큰 문제로 aws설명서가 아니면 자료가 거의 없다는 점이 걸려 firebase로 옮기기로 결정을 했다.
~~~
___
`aws pinpoint directmessaging`      
<img src="https://user-images.githubusercontent.com/38582562/43409530-2129832c-945f-11e8-9fc1-ce89b569e77f.png" width="40%">    
`firebase console cloud messaging`   
<img src="https://user-images.githubusercontent.com/38582562/43438288-70896bf8-94c8-11e8-84c8-7a07d27067e3.png" width="40%">    

___
#180727 token to mysql DB
-
`mysql insert`   
~~~
create table token(
     id int(11) unsigned not null auto_increment,
     token varchar(200) not null,
     primary key (id)
)DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;
~~~

`18년 7월초부터 메서드가 바뀌었다. 기존메서드 사용시 안드로이드스튜디오에서 is deprecated가 뜬다.`
`#FirebaseInstanceId.getInstance().getToken();->`  
`FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener`     
~~~java
  protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener
        ( MainActivity.this,  new OnSuccessListener<InstanceIdResult>() {
            @Override
            public void onSuccess(InstanceIdResult instanceIdResult) {
                String token = instanceIdResult.getToken();
                Log.e("Token",token);
                InsertData task = new InsertData();
                task.execute("http://....../insert.php", token);
            }
        });
    }
~~~

`#onTokenRefresh->onNewToken`   
~~~java
public class MyFirebaseMessagingService extends FirebaseMessagingService {
    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
    }
}
~~~
`+)HttpURLConnection을 이용해 register.php통해 mysql db에 연결해 보았으나`    
`잘 저장되던 것이 몇 시간 후에 보면 안되어있고, 중복저장되는 등 오류가 많아 보류했다.`    
`나중에 추가예정`      

___
#180728 php
-
`firebase.php`   <br>
<img src="https://user-images.githubusercontent.com/38582562/43440858-c5608868-94d3-11e8-95f9-e5d34c1cb73c.PNG" width="60%">    
~~~php
<!doctype html>
<html lang="en">
 <head>
  <meta charset="UTF-8">
  <meta name="Generator" content="EditPlus®">
  <meta name="Author" content="">
  <meta name="Keywords" content="">
  <meta name="Description" content="">
  <title>FCM:php로보내기</title>

  <style type="text/css">
	input {margin: 8px;}
	.messageWrapper {width: 400px;}
  </style>
 </head>
 <body>
  <hr>
   <h1>Goodoc_180728</h1>
  <hr>
 <div class="messageWrapper">
	<form action="push_notification.php" method="post" enctype="multipart/form-data">
		<label for="title_">Title<input id="title_" name="title" size="40" placeholder="제목을 입력하세요" required></input></label><br>
		<label for="message_">Message<textarea name="message" rows="4" cols="60" placeholder="메세지를 입력하세요" required></textarea></label><br>
		<br>
		<input type="submit" name="submit" value="Send" id="submitButton">
	</form>
</div>
<hr>
 </body>
</html>
~~~
___ 
`push_notification.php`   
<img src="https://user-images.githubusercontent.com/38582562/43440859-c58ee118-94d3-11e8-9dea-3144a68d5c81.PNG" width="60%"> 
~~~php
<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />

<?php

	function send_notification ($tokens,/* $message, $data, */$jsonMessage)
	{
		$key = "서버키";
		$url = 'https://fcm.googleapis.com/fcm/send';

		$headers = array(
			'Authorization:key =' . $key,
			'Content-Type: application/json'
			);

	$ch = curl_init();
       curl_setopt($ch, CURLOPT_URL, $url);
       curl_setopt($ch, CURLOPT_POST, true);
       curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
       curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
       curl_setopt ($ch, CURLOPT_SSL_VERIFYHOST, 0);
       curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
       curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode(/**/$jsonMessage));
       $result = curl_exec($ch);
       if ($result === FALSE) {
           die('Curl failed: ' . curl_error($ch));
       }
       curl_close($ch);
       return $result;
	}

  $title = $_POST['title'];
	$msg = $_POST['message'];

  $jsonMessage = array(
  "to" =>"/topics/goodoc",    //###########3토픽!
  "notification" => array("title"=>$title,"body"=>$msg),
  "data"=>array("wow"=>"good")
  );

  $message_status = send_notification($tokens, $jsonMessage);
  echo $message_status;

	echo "<br><br>";
	echo "<button><a href='firebase.php'>돌아가기</a></button>";
 ?>
~~~
___
#180729 token -> topic (알림허용여부)
-
`activity_main.xml에 버튼을 두개 만들어 준 후`    
~~~
  <Button
        android:id="@+id/btn_subscribe"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="SUBSCRIBE" />

    <Button
        android:id="@+id/btn_unsubscribe"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="UNSUBSRIBE" />
~~~
`MainActivity.java에 'goodoc'이라는 주제를 미리 만들어둔다. 주제 등록시까지 최대 하루정도 걸린다고 했는데 나는 바로 적용되었다.`   
~~~
 private final String TOPIC = "goodoc";                                        #goodoc 토픽 선언
 
 btn_subscribe.setOnClickListener(new View.OnClickListener() {                 #SUBSCRIBE버튼을 누르면 알림허용
     @Override
     public void onClick(View view) {
          FirebaseMessaging.getInstance().subscribeToTopic(TOPIC);
 }
  btn_unsubscribe.setOnClickListener(new View.OnClickListener() {              #UNSUBSCRIBE버튼을 누르면 알림거부
     @Override
     public void onClick(View view) {
          FirebaseMessaging.getInstance().unsubscribeFromTopic(TOPIC);
 }
 ~~~
 <img src="https://user-images.githubusercontent.com/38582562/43439273-797ab74e-94cd-11e8-8329-c82288a0bcf3.jpg" width="40%">  

___
#180730 버튼 디폴트 & ~~~푸시 클릭시 새 액티비티~~~ & 버튼클릭시 확인알림창 띄우기
-
`버튼 디폴트//`   
`버튼을 구현했더니 처음에 디폴트값을로 구독이 안된상태로 되어있었다.`   
`생각보다 쉽게 MainActivity.java의 oncreate() 맨위에 올려주는 것만으로도 해결이 되었다.`   
~~~java
 protected void onCreate(Bundle savedInstanceState) {
	 FirebaseMessaging.getInstance().subscribeToTopic(TOPIC);
}
~~~
___
~~~`푸시 클릭시 새 액티비티//`~~~ `->##180804 fcm 전송 형식바뀌면서 다시 안됨`    
<img src="https://user-images.githubusercontent.com/38582562/43440860-c5b4fd44-94d3-11e8-82d4-304de73ec5eb.jpg" width="40%"> 
<img src="https://user-images.githubusercontent.com/38582562/43440861-c5db219a-94d3-11e8-8579-8e1c83edea3d.jpg" width="40%"> 
<br>
~~~`AndroidManifest.xml에 추가해준다.`~~~    
~~~java
 <activity android:name=".NotiActivity">
   <intent-filter>
     <action android:name="OPEN" />
     <category android:name="android.intent.category.DEFAULT"/>
   </intent-filter>
 </activity>
~~~
~~~`NotiActivity를 생성해준다.`~~~    
~~~
public class NotiActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_noti);
    }
}
~~~
~~~`뷰를 만들어준다.`~~~   
~~~java
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:paddingLeft="16dp"
    android:paddingRight="16dp"
    android:orientation="horizontal"
    android:gravity="center">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="알림클릭시뜨는화면" />
</LinearLayout>
~~~
~~~`push_notification.php파일의 jsonMessage에 "click_action"=>"OPEN"를 추가해준다.`~~~   
~~~php
  $jsonMessage = array(
  "to" =>"/topics/goodoc",
  "notification" => array("title"=>$title,"body"=>$msg, #####"click_action"=>"OPEN"),
  "data"=>array("wow"=>"good")
  );
~~~
___
`버튼클릭시 확인알림창 띄우기//`  
<img src="https://user-images.githubusercontent.com/38582562/43441347-7fc15c18-94d5-11e8-9d3b-02af2be556ec.jpg" width="40%"> 
<img src="https://user-images.githubusercontent.com/38582562/43441349-7fe8c2a8-94d5-11e8-8617-13c1d65fcd67.jpg" width="40%"> 
<br>
`MainActivity.java의 onCreate()아래에 버튼리스너에 AlertDialog()를 추가해주면 된다.`   
~~~java
  btn_subscribe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseMessaging.getInstance().subscribeToTopic(TOPIC);
                AlertDialog.Builder ad = new AlertDialog.Builder(MainActivity.this);
                ad.setTitle("알림");       // 제목 설정
                ad.setMessage("허용");   // 내용 설정
                ad.setPositiveButton("Yes");
                ad.show();
            }
        });

        btn_unsubscribe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseMessaging.getInstance().unsubscribeFromTopic(TOPIC);
                AlertDialog.Builder ad = new AlertDialog.Builder(MainActivity.this);
                ad.setTitle("알림");       // 제목 설정
                ad.setMessage("거부");   // 내용 설정
                ad.setPositiveButton("Yes");
                ad.show();
            }
        });
~~~
___
#180731 버튼을 스위치로 바꾸기(SharedPreferences 사용)
-
`//`  
<img src="https://user-images.githubusercontent.com/38582562/43642776-d715e3e0-9763-11e8-86d5-bd0b1340019a.jpg" width="40%"> 
<img src="https://user-images.githubusercontent.com/38582562/43642777-d73f80c4-9763-11e8-827d-96c24f2b71b7.jpg" width="40%"> 
<br>
``     
`activity_main.xml에 스위치를 추가해준다.`          
~~~java
 <Switch
        android:id="@+id/btn_switch"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:checked="true"
        android:text="알림"
        android:textStyle="bold"
        android:switchMinWidth = "70dp"
    />
~~~
`MainActivity에 버튼을 등록하고 스위치 켜짐여부에따라 토픽구독여부(알림여부)를 설정해준다.`   
~~~java
public class MainActivity extends AppCompatActivity {
    private Switch btn_switch;       				  //스위치
    @Override
    protected void onCreate(Bundle savedInstanceState) {
            btn_switch = (Switch) findViewById(R.id.btn_switch);  //스위치 버튼 땡겨오기 
	    
	     btn_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { //스위치
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {                                                //켜져있으면
                    FirebaseMessaging.getInstance().subscribeToTopic(TOPIC);   //알림허용
                }
                else {                                                         //꺼져있으면
                    FirebaseMessaging.getInstance().unsubscribeFromTopic(TOPIC); //알림거부
                }
            }
        });
    }
}
~~~
`하지만, 이렇게만 하면 스위치의 상태가 저장되지 않아 스위치를 끄고 앱을 다시켰을때 켜지는 현상이 벌어진다.`      
`구글링을 하다가 SharedPreferences을 사용해 스위치 상태를 저장하는 법을 발견했다.`    
~~~java
public class MainActivity extends AppCompatActivity {
    private SharedPreferences Pre;              //SharedPreferences 선언
    private SharedPreferences.Editor PreEdit;
    private Switch btn_switch;        //스위치
    @Override
    protected void onCreate(Bundle savedInstanceState) {
            btn_switch = (Switch) findViewById(R.id.btn_switch);  //스위치 버튼 땡겨오기 
	    
	    Pre = getSharedPreferences("setting", 0);      //0으로 초기화 
            PreEdit = Pre.edit();     
	    
	    btn_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { //스위치
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {                                                //켜져있으면
                    PreEdit.putString("switch", "1");                   // 1
                    PreEdit.commit();                                          // SharedPreferences에 저장
                    FirebaseMessaging.getInstance().subscribeToTopic(TOPIC);   //알림허용
                }
                else {                                                         //꺼져있으면
                    PreEdit.putString("switch", "0");                   // 0
                    PreEdit.commit();                                          //SharedPreferences에 저장
                    FirebaseMessaging.getInstance().unsubscribeFromTopic(TOPIC); //알림거부
                }
            }
        });

        if(Pre.getString("switch", "").equals("1")) {                   //어플을 껐다 켰을 때 스위치 상태를 고정하기 위해 
								        //SharedPreferences 내용확인,
            btn_switch.setChecked(true);                                       //1이면 켜짐
        } else {
            btn_switch.setChecked(false);                                      //0이면 꺼짐
        }
    }
}
~~~
___
#180801-03 백그라운드 푸시와 포그라운드 푸시
-
`//`  
<img src="https://user-images.githubusercontent.com/38582562/43645359-95cc8322-976c-11e8-8bcb-de9c74c90397.jpg" width="40%"> 
<img src="https://user-images.githubusercontent.com/38582562/43645360-95f3fcc2-976c-11e8-957f-c99c025bd790.jpg" width="40%"> <br>
`지금까지 내가 구현한 푸시는 단순히 타이틀과 내용을 바꿔서 푸시하는 것이었는데,`    
`다른 어플을 보면 이런식으로 밀어서 내리면 광고이미지가 뜨는 푸시들이 있었다.`     
`구글링 한 결과, BigTextStyle과 BigPictureStyle을 이용하면 된다고 한다.`    
~~~java
.setStyle(new NotificationCompat.BigTextStyle()
       .setBigContentTitle(title)
       .bigText(body))
                       
.setStyle(new NotificationCompat.BigPictureStyle()
       .bigPicture(bigPicture)
       .setBigContentTitle("FCM Push Big Text Title")
       .setSummaryText(body))
~~~
`그런데ㅠㅠ 생각지 못했던 난관이 있었는데,`  
<img src="https://user-images.githubusercontent.com/38582562/43643833-7ff14f60-9767-11e8-91b5-4f56368482a5.PNG" width="40%"> 
<img src="https://user-images.githubusercontent.com/38582562/43643728-4223c3ac-9767-11e8-8c0c-d1e403683b84.jpg" width="40%"> 
<br>
`같은 폼에서 입력해도 백그라운드 푸시와 포그라운드 푸시가 다르게 왔다ㅠㅠ.`    
`원인은 fcm 전송 타입이었는데, 나는 notification와 data가 합쳐진 형식을 사용하고 있었다.`
<br>
___
`#notification`     
~~~
 {
    "to" : "",
    "########notification" : {
      "title" : "",
      "body" : ""
    }
  }
~~~
`#data`      
~~~
{
   "to" : "",
   "########data" : {
     "title" : "",
     "body" : ""
   },
 }
~~~
`#notification + data`    
~~~
{
  "to" : "",
  "priority" : "high",
  "########notification" : {
    "title" : "",
    "body" : ""
  },
  "########data" : {
    "body" : ""
  }
}
~~~
<img src="https://user-images.githubusercontent.com/38582562/43644078-60d386f6-9768-11e8-81e8-094a764b0b5b.jpg" width="80%"> <br>
`위의 표를 보면 notification과 notification+data 형식은`   
`포그라운드에서만 onMessageReceived()를 사용한다는 것을 알 수 있다.`     
`따라서 포그라운드와 백그라운드가 같은 푸시가 오기 위해서는 data형식만 써야한다.`     
`아래 코드는 notification을 어떻게 data로 바꿔야할지 몰라 맨처음에 방황했었던 코드다.`         
~~~php
  $Message = array(
    "to" =>"/topics/goodoc",  							//주제
    "priority"=>"high",	      							//우선순위
    "notification" => array("title"=>$title,"body"=>$msg, "click_action"=>"OPEN_THIS_ACT"),  
    "data"=>array("title"=>$title, "body"=>$msg, "click_action"=>"OPEN_THIS_ACT", "color"=>"#3F51B5")
  );
~~~
`여기서 냅다 notification의 title과 body를 지우면 어플이 오류를 내며 꺼져버렸다.`      
`그래도 결국 어떻게 하다가 notification을 지우고 data값만 남기고,`    
~~~php
  $fields = array(
    'registration_ids' => $tokens,
    'data' => $message   							//data타입으로보내기!
  );
  
  $myMessage = $_POST['message']; 
  $myImgUrl = $_POST['imgurllink']; 

  $message = array("to" =>"/topics/goodoc", "message" => $myMessage, "imgurllink" => $myImgUrl);
  $message_status = send_notification($tokens, $message);
  echo $message_status;
  echo " - okok";
~~~
`onMessageReceived() 코드도 수정했다.`     
~~~java
  sendNotification(remoteMessage.getData().get("title"),remoteMessage.getData().get("message"),                              
         remoteMessage.getData().get("imgurl"));
  Log.d(TAG, "imgurl: " + remoteMessage.getData().get("imgurl"));
~~~
`이젠 백그라운드, 포그라운드, 어플이 완전꺼진상태(killed) 다 메세지가 왔지만,`   
`이번에는 폼에서 입력한 값을 넘겨줄 수 없었다.ㅠㅠ log에도 null이 찍힌다. 아예 넘어오지 않는 것 같다.`   
<img src="https://user-images.githubusercontent.com/38582562/43677320-1ecf1990-983b-11e8-9671-7df9b365ad91.PNG" width="80%"> <br>

`그래서 sendNotification()에 명시적으로 url을 써줘서 불러왔다.`   
~~~java
 String myimgurl="https://.jpg";###############
 try {
    URL url = new URL(########myimgurl);
    bigPicture = BitmapFactory.decodeStream(url.openConnection().getInputStream());
 } catch (Exception e) {
    e.printStackTrace();
 }
~~~
`//`  
<img src="https://user-images.githubusercontent.com/38582562/43677226-4c2fcd46-9839-11e8-9274-a70564fa7a61.jpg" width="40%"> 
<img src="https://user-images.githubusercontent.com/38582562/43677227-4c61ce18-9839-11e8-98bf-ec909ac155b4.jpg" width="40%"> <br>
___
#180804 php->node.js
-
`null값 찍히는 것 때문에 하루종일 고민하다 어차피 팀원들 웹서버가 nodejs라 nodejs 환경으로 옮겨볼까하다`   
`검색한지 10분만에 nodejs가 더 쉽다는 걸 깨달을 수 있었다.... 너무나 간단했어...`    
~~~node.js
var FCM = require('fcm-node');			 // npm install fcm-mode --save 필요
var serverKey = '####서버키';

var push_data = {
    to: '/topics/goodoc', 			  //주제
    priority: "high",				  //우선순위
    data: {
        title: "안녕",			        //#########타이틀
        sound: "default",
        click_action: "OPEN_THIS_ACT"
    }
};

var fcm = new FCM(serverKey);
fcm.send(push_data, function(err, response) {
    if (err) {
        console.error('메시지 발송 실패');
        console.error(err);
        return;
    }
    console.log('메시지 발송 성공');
    console.log(response);
});

~~~
`onMessageReceived() 코드`     
~~~java
  sendNotification(remoteMessage.getData().get("title"),remoteMessage.getData().get("message"),                              
         remoteMessage.getData().get("imgurl"));
  Log.d(TAG, "text: " + remoteMessage.getData().get("text"));
~~~
`라는 짧은 node.js 소스로 cmd창에서 보내주기만 해도`  
<img src="https://user-images.githubusercontent.com/38582562/43677319-1ead4f4a-983b-11e8-8d81-56cc0dbd3e28.PNG" width="80%"> <br>
`이렇게 이쁘게 값이 넘어와 줬다.`  
___
`이제 명시적으로 sendNotification()에 넣어줬던 url값 대신 node.js에 imageurl값을 넣어주고,`   
`sendNotification()를 수정해주면,`   
~~~node.js
var push_data = {
    data: {
       imageurl : "https://.jpg",    //url 추가
    }
}
~~~
~~~java
public void onMessageReceived(RemoteMessage remoteMessage) {
   sendNotification(remoteMessage.getData().get("title"),remoteMessage.getData().get("message"),   
           remoteMessage.getData().get("######imageurl"));
   Log.d(TAG, "imageurl: " + remoteMessage.getData().get("imageurl"));
}
private void sendNotification(String messageTitle,String messageBody, String myimg) {
    try {
            URL url = new URL(######myimg);
            bigPicture = BitmapFactory.decodeStream(url.openConnection().getInputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }
}
~~~
<img src="https://user-images.githubusercontent.com/38582562/43677546-96834a44-983e-11e8-8597-43a14808d4ff.PNG" width="100%"> <br>
`log도 잘 찍히고 포그라운드, 백그라운드, 꺼진상태(killed) 상관없이 이미지와 푸시 모두 잘 들어왔다. 짝짝`   
<br><img src="https://user-images.githubusercontent.com/38582562/43677884-746c72f4-9844-11e8-8987-ca939bfcc23e.jpg" width="40%">
<img src="https://user-images.githubusercontent.com/38582562/43677883-744563d0-9844-11e8-9103-ad6031f96b53.jpg" width="40%"> <br>

___
#180806_이미지 s3로 호스팅해서 보내기, 오레오 진동모드일때만 진동울리는 문제(소리모드일 때 안옴)
-
`이미지 s3로 호스팅해서 보내기//`   
`s3버킷을 생성에 이미지를 올려준 다음, 버킷>이미지>오른쪽클릭>퍼플릭으로설정하면,`   
`https://s3-ap-northeast-1.amazonaws.com/~~~~~~/ad.png`   
`https://s3-(region).amazonaws.com/(bukkit_name)/(filename) url로 끌어올 수 있다.`      
`+)나중에 폼에서 파일넣기로 올리면 aws s3버킷으로 업로드한다음에 그 url로 쏠 수 있으면 좋을 것 같다.`         
<br><img src="https://user-images.githubusercontent.com/38582562/43720079-6cfdff42-99ca-11e8-8dc8-aea147946487.jpg" width="40%"> 
<img src="https://user-images.githubusercontent.com/38582562/43720080-6d281584-99ca-11e8-9ee1-008e40709e4f.jpg" width="40%"> <br>
___
`오레오 진동모드일때만 진동울리는 문제(소리모드일 때 안옴)//`   
`진동이 진동모드일때만 울리고 소리모드일 때는 안울리는 상황이 있었다.`    
`어플 앱정보>알림에 들어가도 진동이 꺼져있어 분명히 안드로이드에서 뭘 건드려야 할 건데`   
`왜 진동모드에선 진동이 울리는가로 고민하다가`  
`NotificationCompat.Builder notificationBuilder가 오레오부터 작동이 안되어 채널을 써야했던 게 기억이 났다.`  <br>  
~~~java
 String channelId = getString(R.string.default_notification_channel_id);                         //이제 Builder사용할 때 channelId필요, strings.xml에 저장되어있음
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.drawable.ic_action_bar)                                         //푸시왔을 때 화면
                        .setContentTitle("FCM_"+title)                                                  //타이틀
                        .setContentText("두 손가락을 이용해 아래로 당겨주세요")                            //텍스트
                        .setAutoCancel(true)                                                            //푸시 누르면 꺼지기
                        .setSound(defaultSoundUri)
                        .setVibrate(new long[]{100, 0, 100, 0})
                        .setStyle(new NotificationCompat.BigPictureStyle()                              //손가락으로 내렸을 때 화면
                                .bigPicture(bigPicture)                                                 //비트맵이미지
                                .setBigContentTitle("FCM_"+title)                                       //타이틀
                                .setSummaryText("* 수신거부 : 설정>알림거부"))                             //텍스트
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {                                           //오레오부터 채널필요
            // Create channel to show notifications.
            String channelName = getString(R.string.default_notification_channel_name);                                                             //strings.xml에 저장되어있음
            NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
            ####channel.enableVibration(true);                                                               //진동
            ####channel.setVibrationPattern(new long[]{100, 200, 100, 200});
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            notificationManager.createNotificationChannel(channel);
        }
        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
~~~
~~~java
 channel.enableVibration(true);                                
 channel.setVibrationPattern(new long[]{100, 200, 100, 200});
~~~
`채널로 .enableVibration과 .setVibrationPattern을 넣어주니 앱정보 진동도 켜지고 소리모드, 진동모드시 진동이 잘 왔다.`  
<br><img src="https://user-images.githubusercontent.com/38582562/43721213-2e01721c-99cd-11e8-90f4-d4cdd63398a4.jpg" width="40%"> 
<img src="https://user-images.githubusercontent.com/38582562/43721214-2e2bc864-99cd-11e8-9458-bf3dbbf80b5e.jpg" width="40%"> <br>

___
#180807_어플깔았을때 스위치 초기화
-
`어플깔았을때 스위치 초기화//`   
`SharedPreferences로 스위치 상태는 잘 저장되는데, 처음 어플 설치시 스위치가 꺼져있는 상태가 디폴트로 되어있었다.`      
`res>layout>activity_main.xml에서 스위치 상태가 아무리 true라도`   <br>
~~~java
<Switch
        android:id="@+id/btn_switch"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        ##############android:checked="true"
        android:text="알림"
        android:textStyle="bold"
        android:typeface="monospace"
        android:switchMinWidth = "70dp"
    />
~~~
`안켜져 있었고, `   
~~~java
btn_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { //스위치
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {                                                //키면
                    SwitchPreEdit.putString("switch", "1");                   // 1
                    SwitchPreEdit.commit();                                          // SharedPreferences에 저장
                    FirebaseMessaging.getInstance().subscribeToTopic(TOPIC);   //알림허용
                }
                else {                                                         //끄면
                    SwitchPreEdit.putString("switch", "0");                   // 0
                    SwitchPreEdit.commit();                                          // SharedPreferences에 저장
                    FirebaseMessaging.getInstance().unsubscribeFromTopic(TOPIC); //알림거부
                }
            }
        });

        if(SwitchPre.getString("switch", "").equals("1")) {                   //어플을 껐다 켰을 때 스위치 상태를 적용하기 위해 SharedPreferences 내용확인,
            btn_switch.setChecked(true);                                       //1이면 켜짐
        } else {
            btn_switch.setChecked(false);                                      //0이면 꺼짐
        }
~~~
~~~java
 SwitchPre = getSharedPreferences("setting", 0);
~~~
`SwitchPre 선언할때의 저 0이 0(false)으로 초기화된 게 아닐까라고 생각했었는데,`   
`찾아보니 저 0은 SharedPreferences 읽기, 쓰기 가능이라는 뜻이었다.`       
~~~
0 : 읽기, 쓰기 가능
MODE_WORLD_READABLE : 읽기 공유
MODE_WORLD_WRITEABLE : 쓰기 공유
~~~
`그렇다고 코드의 맨 위에서 명시적으로 켜주기엔 어플 킬때마다 스위치가 켜지는...데(스위치 상태저장이 안됨)`    
`결국 빙빙돌다가 어플 제일 처음설치시 스위치를 켜주면 되겠다라고 생각이 들었고, SharedPreferences을 하나 더 쓰면 된다는 것을 찾을 수 있었다.`
~~~java
FirstPre = getSharedPreferences("isFirst", MainActivity.MODE_PRIVATE);
        FirstPreEdit = FirstPre.edit();
        boolean first = FirstPre.getBoolean("isFirst", false);  	//최초 실행시 switch on(==푸시허용)
        if(first==false){
            FirstPreEdit.putBoolean("isFirst",true);
            SwitchPreEdit.putString("switch", "1");			//스위치값을 1로 (==on)
            SwitchPreEdit.commit();					//저장
            FirstPreEdit.commit();
            FirebaseMessaging.getInstance().subscribeToTopic(TOPIC);	//알림허용
        }
~~~
___
#180807-09_nodejs로 완전히 이동, 새로고침시 안 바뀌게 ajax사용
-
`nodejs의 웹서버만 간신히 돌리고 post로 html 폼을 연결할 수 있는 상태에서`    
`폼에서 제출버튼을 누르면 push_notification.js를 모듈로 끌어와 실행하는 것까지는 할 수 있었다.`     
`다만, 문제가 폼을 제출하면 푸시가 보내지면서 동시에 흰 화면이 나와서 다시 새로고침을 해 줘야 하는 문제가 있었다.`    
`php때 처럼 html 페이지를 하나 더 만들어 보내졌습니다. 하고 뒤로가기 버튼을 만들어 줄까 하다가`   
`'ajax 비동기처리하는게 어때'라는 힌트를 얻었다. 구글링해도 시원치않았고 어떻게 하다가 고민하다가` 
https://www.inflearn.com/course-status-2/
`섹션1과 섹션2의 강의에서 많은 도움을 받았다. `    
<img src="https://user-images.githubusercontent.com/38582562/43959867-30773a74-9ceb-11e8-957f-2662ede1d3b2.PNG" width="40%"><br>
~~~node.js
  <body>
    <div class="messageForm" style="border: 3px solid gray; width: 500px;">
      <form action="" method="post">
        <input type="button" value="Send" id="Send" class="ajaxsend"> ################
      </form>
    </div>
    <br>
    <div style="border: 3px solid gray; width: 500px;">
      <div class="result"></div><hr>        <!-- 얻은 title값 표시 --> ################
      <div class="result2"></div>           <!-- 얻은 url값 표시 --> ################
    </div>
  </body>
~~~
`body 태그 안에 버튼을 만들어주고 title과 url이 잘 입력될 수 있는지 표시할 수 있는 div태그를 만들어준다.`
~~~node.js
<script>
      document.querySelector('.ajaxsend').addEventListener('click', function(){
        var title_inputdata = document.forms[0].elements[0].value; //폼의 첫번째 엘리먼트(input)의 value값을 가져옴 == title
        var url_inputdata = document.forms[0].elements[1].value;   //폼의 두번째 엘리먼트(input)의 value값을 가져옴 == url

        sendAjax('http://localhost:3000/ajax_send', title_inputdata);
        sendAjax2('http://localhost:3000/ajax_send', url_inputdata);
        alert('전송되었습니다.');
      })

      function sendAjax(url, data){
          var data = {'title':data};
          data = JSON.stringify(data);  //string형태로 바꿔줌

          var xhr = new XMLHttpRequest();
          xhr.open('POST',url);
          xhr.setRequestHeader('Content-Type',"application/json");
          xhr.send(data);

          xhr.addEventListener('load',function(){
            var result = JSON.parse(xhr.responseText);
            document.querySelector(".result").innerHTML= result.title;
          });
        }

        function sendAjax2(url, data){
            var data = {'url':data};
            data = JSON.stringify(data);

            var xhr = new XMLHttpRequest();
            xhr.open('POST',url);
            xhr.setRequestHeader('Content-Type',"application/json");
            xhr.send(data);                                           //이부분 땜에 sendAjaxd와 sendAjax2 둘 못합침

            xhr.addEventListener('load',function(){
              var result = JSON.parse(xhr.responseText);
              document.querySelector(".result2").innerHTML= result.url;
            });
          }
    </script>
~~~
`그 다음, 전송버튼을 누르면 전송되었다고 alert를 울려주고, title값과 url값을 각각 sendAjax()와 sendAjax2()로 보내준다.`  
`두 함수를 합칠 수 있을 것 같았는데  'xhr.send(data);' 부분 때문에 따로 따로 보내줬다.`   
`나중에 nodejs와 좀 더 친숙해지면 많이 고쳐봐야겠다.`   

___
`180810까지의 결과`  
___
<br><img src="https://user-images.githubusercontent.com/38582562/43959868-309b07b0-9ceb-11e8-8d58-8b8f526043dc.jpg" width="30%"> 
<img src="https://user-images.githubusercontent.com/38582562/43959869-30bff610-9ceb-11e8-90e8-340b9f26c975.jpg" width="30%">
<img src="https://user-images.githubusercontent.com/38582562/43960513-383457d6-9ced-11e8-85d3-2ebad948a75d.jpg" width="30%"> <br>

___
#181120 푸시왔을시 화면깨우기
-
`http://devmason.tistory.com/146의 도움을 받았다.`  
`AndroidManifest.xml`
~~~xml
<uses-permission android:name="android.permission.WAKE_LOCK" />
~~~
`PushUtils.java`
~~~java
public class PushUtils {
    private static final String TAG = PushUtils.class.getSimpleName();

    private static PowerManager.WakeLock mWakeLock;

    public static void acquireWakeLock(Context context){
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(
                PowerManager.FULL_WAKE_LOCK |
                        PowerManager.ACQUIRE_CAUSES_WAKEUP |
                        PowerManager.ON_AFTER_RELEASE, "WAKEUP"
        );

        mWakeLock.acquire();
    }

    public static void releaseWakeLock(){
        if(mWakeLock != null){
            mWakeLock.release();
            mWakeLock = null;
        }
    }
}
~~~
`FirebaseMessagingService를 상속받은 MyFirebaseMessagingService.java 에 acquireWakeLock/releaseWakeLock 추가`
~~~java
public class MyFirebaseMessagingService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        PushUtils.acquireWakeLock(this); ///////////
    }

    private void sendNotification_alert(String title, String body) {
        PushUtils.releaseWakeLock(); ///////////
        ....
    }
    private void sendNotification(String title, String myimg) {
        PushUtils.releaseWakeLock(); ///////////
       ....
}
~~~
___
#181120 광고알림(이미지+텍스트)과 주문상태알림(텍스트만) 구분해서 안드로이드단에서 받기
-
`다른 기능들과 합치며 어플을 완성하다보니 기존의 광고알림뿐 아니라 매장에서 주문상태를 변경하면 알려주는 상태알림 푸시도 필요했다. 하지만, 두 푸시 다 발송대상(to)도 다르고 보내는 데이타 형식이 다른데 같은 메서드(sendNotification)에서 받자니 갑갑했다.`

`기존의 광고알림 전송코드 중 일부(node.js)`
~~~js
  var push_data = {
      to: '/topics/goodoc',             //topic
      priority: "high",           
      data: {                      
          title: title,                 //제목
          imageurl : imgurllink,        //이미지url
          sound: "default"
          //"clickAction": "NotiActivity" 
      }
  };
~~~
`상태알림 코드`
~~~js
  var push_data = {
      to: token,                        //token
      priority: "high",           
      data: {                         
          title: title,                 //제목
          body: body,			//내용
          sound: "default"
          //"clickAction": "NotiActivity"
      }
  };
~~~
`node.js는 firebase 콘솔이 아닌 서버에서 단순히 발송을 해주는 역할이라 바꿀 코드 자체가 없었고 바꾸려면 안드로이드 스튜디오에서  MyFirebaseMessagingService.java를 고치는 방법이 없는데 data: {} 안의 형식을 어떻게 구분할까하다가 remoteMessage.getData().get("title")로 title값을 받아 sendNotification함수로 넘겨주는 것을 보았다.`
~~~java
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        ...
        sendNotification(remoteMessage.getData().get("title"), remoteMessage.getData().get("imageurl")); ////
    }
 ~~~
`remoteMessage.getData().get("imageurl")==null 여부로 구분해 연결되는 함수로 나눠주어 NotificationCompat.Builder notificationBuilder로 세팅을 다르게 해주었다.`
~~~java
   if(remoteMessage.getData().get("imageurl")==null){ //node 서버에서 온 imageurl값이 null이면 알림으로 인식,
                                                      //sendNotification_alert로
      sendNotification_alert(remoteMessage.getData().get("title"), remoteMessage.getData().get("body"));
   }else{                                            //imageurl값이 있으면 광고로 인식, sendNotification_ad로
      sendNotification(remoteMessage.getData().get("title"), remoteMessage.getData().get("imageurl"));
   }
~~~
___
`181120까지의 결과`  
___
<br><img src="https://user-images.githubusercontent.com/38582562/49933014-ed8e8c00-ff0d-11e8-900c-88d4f45e7428.jpg" width="30%"> 
<img src="https://user-images.githubusercontent.com/38582562/49933015-ed8e8c00-ff0d-11e8-9feb-7570505b78df.jpg" width="30%">
<img src="https://user-images.githubusercontent.com/38582562/49933017-ed8e8c00-ff0d-11e8-9c17-99327ea843be.jpg" width="30%"> <br>
