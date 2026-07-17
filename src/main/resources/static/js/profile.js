$(function(){
	$(".follow-btn").click(follow);
});

function follow() {
	var btn = this;
    var path = $(btn).hasClass("btn-info") ? "/follow" : "/unfollow";
    $.post(
        CONTEXT_PATH + path,
        {"entityType":3,"entityId":$(btn).prev().val()},
        function(data){
            if(data.code === 0){
                window.location.reload();
            }else {
                alert(data.msg);
            }
        },
        "json"
    );
}
