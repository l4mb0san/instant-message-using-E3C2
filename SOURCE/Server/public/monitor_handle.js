var socket = io ("https://anonymous-messaging-app.herokuapp.com/");

$(document).ready(function(){
});

socket.on('connection_monitor', function(data) {
  $("#list").append("<div class='user'>" + data + "</div>")
});
