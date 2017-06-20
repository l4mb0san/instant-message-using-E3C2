var socket = io ("https://anonymous-messaging-app.herokuapp.com/");

$(document).ready(function(){
  socket.emit('join-room-monitor');
  $('#message-body > tbody:last-child > tr').remove();
  $('#list-online-body > tbody > tr').remove();
});

socket.on('list-online-monitor', function(data){
  $('#list-online-body > tbody > tr').remove();
  for(key in data) {
    if(data.hasOwnProperty(key)) {
        var value = data[key];
        var name = value.NAME;
        var avatar = value.AVATAR;
        $('#list-online-body > tbody:last-child').append("<tr><td>" + '<img style="width:30px; height:30px;" src="' + avatar + '"/> ' + name + "</td></tr>");
    }
  }
});

socket.on('send-message-monitor', function(data){
  var sender = data._sender;
  var message = data._message;
  $('#message-body > tbody:last-child').append("<tr><td>" + '<img style="width:30px; height:30px;" src="' + sender + '"/> ' + message + "</td></tr>");
});

function scrollToBottom() {
  $('tbody').scrollTop = messages.scrollHeight;
}
