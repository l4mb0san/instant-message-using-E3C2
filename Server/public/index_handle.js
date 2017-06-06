var socket = io ("http://localhost:3000");

$(document).ready(function() {
  $(".current-date").html(new Date().toLocaleDateString());
});

socket.on("server-send-list-online", function(data) {
  $(".user-list").remove();

  var tag1 = "<li class='user-list'><a href='#'>"
    + "<i class='fa fa-plus'></i>"
    + "<span class='menu-title'>";
  var tag2 = "</span></a></li>";

  data.forEach(function(i) {
    $("#side-menu").append(tag1 + i + tag2);
  });
});

socket.on("server-send-message", function(data){

  var tag1 = "<li class='in'>"
    + "<img src='https://s3.amazonaws.com/uifaces/faces/twitter/kolage/48.jpg'"
    +     "class='avatar img-responsive'/>"
    +  "<div class='message'  style='width: 75%'><span class='chat-arrow'></span>"
    +  "<a href='#'class='chat-name'>";
  var tag2 = "</a>&nbsp;<span class='chat-datetime'>";
  var tag3 = "</span><span class='chat-body'>";
  var tag4 = "</span></div></li>";

  $("#chat").append(tag1 + data.username
    + tag2 + new Date().toLocaleString()
    + tag3 + data.message + tag4);

  Scroll();
});

function Scroll() {
  window.setInterval(function() {
    var elem = document.getElementById('chat');
    elem.scrollTop = elem.scrollHeight;
  }, 5000);
}
