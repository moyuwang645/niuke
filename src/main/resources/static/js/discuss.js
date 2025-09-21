$(function() {
    $(topBtn).click(setTop);
    $(wonderfulBtn).click(setwonderful);
    $(deleteBtn).click(setdelete);
})
function like(btn,entityType,entityId,entityUserId){
    $.post(
        CONTEXT_PATH + '/like',
        {"entityType=":entityType,"entityId":entityId,"entityUserId":entityUserId},
        function(data){
            data = JSON.parse(data);
            if(data.code==0){
                $(btn).children("i").text(data.likeCount);
                $(btn).children("b").text(data.likeStatus==1?"已赞":"赞");
            }else{
                alert(data.message);
            }
        }

    )
}

function settop(){
    $.post(
        CONTEXT_PATH + '/discuss/top',
        {"id":$("#postId").val()},
        function(data){
        data = JSON.parse(data);
        if(data.code==0){
            $("#topBtn").attr("disabled","disabled");
        }else{
            alert(data.message);
        }
        }
    )
}

function setwonderful(){
    $.post(
        CONTEXT_PATH + '/discuss/wonderful',
        {"id":$("#postId").val()},
        function(data){
            data = JSON.parse(data);
            if(data.code==0){
                $("#wonderfulBtn").attr("disabled","disabled");
            }else{
                alert(data.message);
            }
        }
    )
}

function setDelete(){
    $.post(
        CONTEXT_PATH + '/discuss/delete',
        {"id":$("#postId").val()},
        function(data){
            data = JSON.parse(data);
            if(data.code==0){
                location.href=CONTEXT_PATH + "index";
            }else{
                alert(data.message);
            }
        }
    )
}