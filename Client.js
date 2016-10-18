/**
 * Created by Rob on 18/10/2016.
 */


$( document ).ready(function() {
    setupPage();
});
function setupPage() {
    var query = window.location.search.substring(1);
    if (query.length == 0) {
        setupLogin();
    } else {
        console.log("Line id:"+query.split("&")[0]);
        console.log("User id:"+query.split("&")[1]);
        window.roomid = "";
        setupMessage();
    }
}
function loginFunc() {
    var server = $("#server").val();
    var port = $("#port").val();
    var user = $("#user").val();
    var auth = $("#auth").val();
    var pass = $("#password").val();
    if(server == "" || port == "" || user == "" || auth == "" || pass == ""){
        alert("Please fill in all fields.")
    } else {
        var server_vals = "" + server + ":" + port.toString();
        var user_vals = flat_msg({
            "type":"newidentity",
            "identity":user,
            "auth":auth,
            "pass":pass
        });
        setup_post(server_vals, user_vals, user);
    }
}
function poll(){
    var response;
    var url = "http://127.0.0.1/poll";
    var id = window.location.search.substring(1).split("&")[0];
    var data = {c:id};
    $.ajax({
        type: "POST",
        url: url,
        data: data,
        dataType: "json",
        success: function (data) {
            processMessages(data["messages"]);
            setTimeout(poll, 3000);
        },
        error: function (data) {
            console.error("Poll failed!");
        }
    });
}
function redirect_to_chat(id,user) {
    window.location.href = window.location.href+"?"+id+"&"+user;
}
function setup_post(message, user_vals, uid) {
    var id = "";
    var url = "http://127.0.0.1/setup";
    var data = {"c":message};
    $.ajax({
        type: "POST",
        url: url,
        data: data,
        dataType: "json",
        success: function (data, status) {
            if(status != "success"){
                alert("Server error, please refresh page!");
            } else {
                id = data["id"];
                my_post(user_vals, id, false);
                redirect_to_chat(id,uid);
            }
        },
        error: function (data, status) {
            alert("Server error, please refresh page!");
        }
    });
}
function my_post(object,id, syn = true) {
    var data = {c: id + object};
    var url = "http://127.0.0.1/content";
    $.ajax({
        type: "POST",
        url: url,
        data: data,
        dataType: "json",
        async: syn,
        success: function (data) {},
        error: function (data) {
            alert("Content push failed!");
        }
    });
}
function setupLogin() {
    $("#chat").hide();
    $("#login").show();
    $("#log").on("click", loginFunc);
    console.log("login document loaded");
}


function setupMessage() {
    $("#chat").show();
    $("#login").hide();
    var message_box = $('#message');
    message_box.keydown(function(event) {
        if (event.keyCode == 13) {
            var message = message_box.val();
            message_box.val("");
            sendMessage(message);
        }
    });
    setTimeout(poll, 500);
    console.log( "message document loaded" );
}

function flat_msg(obj) {
    var content = "";
    for(var key in obj){
        content = content +"\"" + key + "\":\"" + obj[key] + "\","
    }
    content = content.substring(0,content.length - 1);
    return "{" + content + "}";
}

function processCommand(message){
    var id = window.location.search.substring(1).split("&")[0];
    var command = message.split(" ")[0];
    var send = {};
    switch(command) {
        case "#list":
            send["type"] = "list";
            break;
        case "#createroom":
            if (message.split(" ").length != 2) {
                errorToUser("#createroom", "SYNTAX #createroom roomid");
            } else {
                send["type"] = "createroom";
                send["roomid"] = message.split(" ")[1];
            }
            break;
        case "#joinroom":
            if (message.split(" ").length != 2) {
                errorToUser("#joinroom", "SYNTAX #joinroom roomid");
            } else {
                send["type"] = "join";
                send["roomid"] = message.split(" ")[1];
            }
            break;
        case "#deleteroom":
            if (message.split(" ").length != 2) {
                errorToUser("#deleteroom", "SYNTAX #deleteroom roomid");
            } else {
                send["type"] = "deleteroom";
                send["roomid"] = message.split(" ")[1];
            }
            break;
        case "#who":
            send["type"] = "who";
            break;
        case "#quit":
            send["type"] = "quit";
            break;
        case "#addserver":
            if (message.split(" ").length != 6) {
                errorToUser("#addserver", "SYNTAX #addserver id address clientPort coordinationPort sysadminPassword");
            } else {
                send["type"] = "addserver";
                send["serverid"] = message.split(" ")[1];
                send["serveraddress"] = message.split(" ")[2];
                send["clientport"] = message.split(" ")[3];
                send["coordinationport"] = message.split(" ")[4];
                send["password"] = message.split(" ")[5];
            }
            break;
        default:
            alert("The command \"" + command + "\" isn't a valid command!")
    }
    if (send != {}){
        var sendString = flat_msg(send)
        my_post(sendString,id)
    }
}

function sendMessage(message) {
    var id = window.location.search.substring(1).split("&")[0];
    if(message.charAt(0) == "#"){
        processCommand(message);
    } else {
        msg = {
            "type":"message",
            "content":message
        };
        var sendString = flat_msg(msg);
        my_post(sendString,id)
    }
}
function errorToUser(element,message) {
    $("#message_area").append("ERROR in \""+element+"\":"+message+ "\n");
}
function toUser(message) {
    $("#message_area").append(message+ "\n");
}
function processMessages(list) {
    for(var msg in list){
        processMessage(list[msg]);
    }
}
function processMessage(msg) {
    switch(msg["type"]){
        case "newidentity":
            if(msg["approved"] == "true"){
                window.user_identity = window.location.search.substring(1).split("&")[1];
                toUser("You have been accepted by the server");
            } else{
                toUser("You were refused by the server");
                alert("Press the back button and try to login with correct credentials");
            }
            break;
        case "roomchange":
            var user = msg["identity"];
            var former = msg["former"];
            var room = msg["roomid"];
            if(user == window.user_identity){
                window.roomid = room;
            }
            toUser(user + " left \"" + former + "\" and joined \"" + room + "\"");
            break;
        case "message":
            var user = msg["identity"];
            var body = msg["content"];
            toUser(user + ": " + body);
            break;
        case "roomlist":
            var list = msg["rooms"];
            var list_string = "";
            for(var item in list){
                list_string = list_string + ", " + list[item];
            }
            list_string = list_string.substr(2,list_string.length-2);
            toUser("Available rooms are: " + list_string);
            break;
        case "createroom":
            var success = msg["approved"] == "true";
            var room = msg["roomid"];
            if(success){
                toUser("Room \"" + room + "\" was successfully created")
            } else{
                toUser("Room \"" + room + "\" was not created")
            }
            break;
        case "serverchange":
            var server = msg["serverid"];
            toUser("You have been moved to \"" + server + "\"");
            break;
        case "deleteroom":
            var room = msg["roomid"];
            var success = msg["approved"] == "true";
            if(success){
                toUser("\"" + room + "\" was successfully deleted");
            } else{
                toUser("\"" + room + "\" wasn't deleted");
            }
            break;
        case "roomcontents":
            var room = msg["roomid"];
            var list = msg["identities"];
            var owner = msg["owner"];
            var list_string = "";
            for(var item in list){
                list_string = list_string + ", " + list[item];
            }
            list_string = list_string.substr(2,list_string.length-2);
            toUser("This room is owned by " + owner + " the following people are in the room: " + list_string);
            break;
        case "route":
            var room = msg["roomid"];
            var host = msg["host"];
            var port = msg["port"];
            moveJoin(room,host,port);
            break;
        case "addserver":
            var server = msg["serverid"];
            var approved = msg["approved"] == "true";
            if(approved){
                toUser("Server '" + server + "' was added")
            }else{
                toUser("Server '" + server + "' was not added")
            }
            break;
        default:
            toUser(flat_msg(msg));
    }
}
function moveJoin(room,host,port){
    var user = window.user_identity;
    var id = window.location.search.substring(1).split("&")[0];
    var message = {
        "type":"movejoin",
        "former": window.roomid,
        "roomid": room,
        "identity": user
    };
    toUser("Moving you to another server please wait...");
    rerouteConnection(host,port,id);
    my_post(flat_msg(message),id,false);
}
function rerouteConnection(host, port, id) {
    var data = {"c":id+host+":"+port};
    var url = "http://127.0.0.1/reroute";
    $.ajax({
        type: "POST",
        url: url,
        data: data,
        dataType: "json",
        async: false,
        success: function (data, status) {
            if(status != "success"){
                alert("Server error, please refresh page!");
            } else {}
        },
        error: function (data, status) {
            alert("Server error, please refresh page!");
        }
    });
}