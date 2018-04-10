package KModbus

import java.nio.ByteBuffer


data class Polling(
        var Enable  :Boolean = true,
        var Health  :Boolean = true,
        var TimeOutCnt   :Int  = 0,
        var StationID    :Int  = 1,
        var StartAddr    :Int  = 0,
        var NumbTodo     :Int  = 5,
        var ShouldReturn :Int  = 0,
        var DevCmd:MdCmdEnum   = MdCmdEnum.RD_WORD
)

data class NowPolling(
        var polling :Polling?,
        var station :Station?
)

interface ModbusCtrls {
    fun IDCheck   (id:Int):Boolean
    fun DBRead    (id:Int,dbType:MdRegion,addr:Int):Int
    fun DBWrite   (id:Int,dbType:MdRegion,addr:Int,data:Short):Boolean
    fun HeaderFind(buffer: ByteBuffer):Boolean
}

open class Station(
        var PollTable :List<Polling> = listOf(),
        var NowPoll   :Polling       = Polling(),
        var IsSlave   :Boolean,
        var Ctrl      :ModbusCtrls
)



open class StationBus(
        var Stations  :List<Station> = listOf(),
        var RawBytes  :ByteArray,
        var RawPkgLen :Int,
        var RspPkgLen :Int,
        var Serial    :Int = 33666, //only for Tcp
        val NowPoll   :NowPolling = NowPolling(null,null)
)


data class DealResponse(
        val code:MdRspCode   = MdRspCode.MD_ERR_OK,
        val rsp :ByteArray   = ByteArray(0)
)





















