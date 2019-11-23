var secKillObj= {
    url: {
        getSystemTime: function () {
            return "/getSystemTime";
        },
        getRandomName:function (goodsId) {
            return "/getRandomName/"+goodsId;
        },
        secKill:function(goodsId,randomName){
            return "/seckill/"+goodsId+"/"+randomName;
        }
    },
    fun: {
        //秒杀的初始化方法，用于控制秒杀抢购按钮的样式行为效果
        initSeckill: function (goodsId, startTime, endTime) {
            $.ajax({
                url: secKillObj.url.getSystemTime(),
                type: "get",
                dataType: "json",
                success: function (currentTime) {
                    console.log(currentTime);
                    console.log(startTime);
                    console.log(endTime);
                    if (currentTime < startTime) {

                        secKillObj.fun.secKillCountdown(startTime * 1, goodsId)

                        return false;
                    }
                    if (currentTime > endTime) {
                        $("#secKillSpan").html('<span style="color: red">当前商品秒杀活动已结束</span>')
                        return false
                    }

                    secKillObj.fun.doSecKill(goodsId);
                },
                error: function () {
                    alert("网络繁忙！请稍后再试！1111111")
                }
            })
        },
        secKillCountdown: function (startTime, goodsId) {
            var killTime = new Date(startTime);
            //使用jQuery的对象调用countdown函数完成倒计时
            //参数1 为倒计时的结束时间
            //参数2 为倒计时的回调方法 ，每秒中会指定一次这个回调用于显示剩余时间
            $("#secKillSpan").countdown(killTime, function (event) {
                //时间格式
                var format = event.strftime('距秒杀开始还有: %D天 %H时 %M分 %S秒');
                $("#secKillSpan").html("<span style='color:red;'>" + format + "</span>");
            }).on('finish.countdown', function () {
                //倒计时结束后回调事件，已经开始秒杀，用户可以进行秒杀了，有两种方式：
                //1、刷新当前页面
                // location.reload();//不能刷新页面，因为秒杀用户可能很多人在等待倒计时结束这时可能会同时产生很多的请求导致高并发的出现
                //或者2、调用秒杀开始的函数
                secKillObj.fun.doSecKill(goodsId)
            });
        },
        doSecKill:function (goodsId) {
            $("#secKillSpan").html('<input type="button" id="secKillBut" value="立即抢购">')
            $("#secKillBut").bind("click",function () {
                $(this).attr("disabled",true);
                $.ajax({
                    url:secKillObj.url.getRandomName(goodsId),
                    type:"get",
                    dataType:"json",
                    success:function (data) {

                        if(data.code!="0"){
                            console.log(data.message)
                            return false;
                        }

                        secKillObj.fun.secKill(goodsId,data.result)
                    },
                    error:function () {
                        alert("网络繁忙！请稍后再试！333333")
                    }
                })
            })
        },
        secKill:function(goodsId,randomName){
            $.ajax({
                url:secKillObj.url.secKill(goodsId,randomName),
                type:"get",
                dataType:"json",
                success:function(data){
                   console.log(data)
                },
                error:function(){
                    alert("网络繁忙！请稍后再试！333333")
                }
            })
        }
    }
}