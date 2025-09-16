$(function(){
	$(".follow-btn").click(follow);
});

function follow() {
	var btn = this;
    if($(btn).hasClass("btn-info")) {
    $.post(
        CONTEXT_PATH + "/follow",
        {"entityType":3,"entityId":$(btn).prev().val()},
        function(data){
            data = JSON.parse(data);
            if(data.code==0){
                window.location.reload();
            }else {
                alert(data.msg);
            }
        }
    )
	} else {
        $.post(
            CONTEXT_PATH + "/unfollow",
            {"entityType":3,"entityId":$(btn).prev().val()},
            function(data){
                data = JSON.parse(data);
                if(data.code==0){
                    window.location.reload();
                }else {
                    alert(data.msg);
                }
            }
	}
}