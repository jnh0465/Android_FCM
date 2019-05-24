//fcm 푸시 모듈

module.exports.send = function (title,token,body){  //모듈 exports
  // --> npm install fcm-mode --save 설치필요
  var FCM = require('fcm-node');
  var serverKey = '';

  var push_data = {
      to: token,             //token
      priority: "high",                 //우선순위
      data: {                           //백그라운드에서 noti받으려면 notification:{} 없이 data로만
          title: title,                 //제목
          body: body,
          sound: "default"
          //"clickAction": "NotiActivity" //푸시알림 터치시
      }
  };

  var fcm = new FCM(serverKey);
  fcm.send(push_data, function(err, response) {
      if (err) {
          //console.error('메시지 발송 실패');
          console.error(err);
          return;
      }
      //console.log('메시지 발송 성공');
      //console.log(response);
  });
}
