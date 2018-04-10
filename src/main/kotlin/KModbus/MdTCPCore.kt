package KModbus

import Base.getBit
import Base.getInt
import Base.toUInt
import Base.toUnsigned
import java.nio.ByteBuffer


fun IsThisResponseTcp(bus:StationBus): Boolean {

    //TCP mode don't have CRC check , so use polling info to decide the package is a response or not
    val serial = bus.RawBytes.getInt(TCP_SERIAL_INDEX)
    if (serial == bus.Serial){
        if(bus.NowPoll.polling != null && bus.NowPoll.station != null){
            if(bus.RawPkgLen == bus.NowPoll.polling!!.ShouldReturn){
                val id = bus.RawBytes[TCP_STATION_NUMB_INDEX].toInt()
                if(id == bus.NowPoll.polling!!.StationID){
                    val cmd = MdCmdEnum.parse(bus.RawBytes[TCP_PKG_CMD_INDEX])
                    if(cmd == bus.NowPoll.polling!!.DevCmd){
                        return true
                    }
                }
            }
        }
    }

    return false
}




fun KM_TCP_Data_Deal(bus: StationBus): DealResponse {
    val sid        = bus.RawBytes[TCP_STATION_NUMB_INDEX].toInt()
    val byteBuffer = ByteBuffer.wrap(bus.RawBytes)
    //1,IDCheck
    val st2find:Station? = bus.Stations.find { it.Ctrl.IDCheck(sid) } ?: return DealResponse(code = MdRspCode.MD_DEV_ADDR_ERR)

    val st = st2find!!

    //2,Cmd Check
    if(bus.RawBytes[TCP_PKG_CMD_INDEX].toInt() !in MdSupportCmd){
        return DealResponse(code = MdRspCode.MD_CMD_ERR)
    }
    //3,no CrcCheck use length check instead
    val pkgLen = bus.RawBytes[TCP_PKG_LEN_INDEX].toUnsigned()*256+bus.RawBytes[TCP_PKG_LEN_INDEX+1].toUnsigned()+TCP_HEADER_LEN
    if(pkgLen != bus.RawPkgLen){
        return DealResponse(code = MdRspCode.MD_LEN_ERR)
    }

    //4,check if is a IsThisResponseTcp
    if(!IsThisResponseTcp(bus)){
        //this is a Request
        val cmd     = MdCmdEnum.parse(bus.RawBytes[TCP_PKG_CMD_INDEX])
        val trgAddr: Int
        val trgLen:  Int
        when (cmd) {
            MdCmdEnum.WR_BITS_ONE  -> {
                trgAddr = byteBuffer.getShort(TCP_REQ_TRG_ADDR_INDEX).toInt()
                val tmp = byteBuffer.get(TCP_REQ_SINGLE_DATA_INDEX).toUnsigned()
                st.Ctrl.DBWrite(sid, cmd.toRegion(), trgAddr, tmp)
            }
            MdCmdEnum.WR_WORD_ONE  -> {
                trgAddr = byteBuffer.getShort(TCP_REQ_TRG_ADDR_INDEX).toUInt()
                val tmp = byteBuffer.getShort(TCP_REQ_SINGLE_DATA_INDEX)
                st.Ctrl.DBWrite(sid, cmd.toRegion(), trgAddr, tmp)
            }
            MdCmdEnum.WR_WORD_SOME -> {
                trgAddr = byteBuffer.getShort(TCP_REQ_TRG_ADDR_INDEX).toUInt()
                trgLen = byteBuffer.getShort(TCP_REQ_TRG_NUMB_INDEX).toUInt()
                for (tmp in 0..(trgLen - 1)) {
                    val data = byteBuffer.getShort(tmp * 2 + TCP_REQ_TRG_DATA_INDEX)
                    st.Ctrl.DBWrite(sid, cmd.toRegion(), trgAddr + tmp, data)
                }
            }
            MdCmdEnum.WR_BITS_SOME -> {
                trgAddr = byteBuffer.getShort(TCP_REQ_TRG_ADDR_INDEX).toInt()
                trgLen = byteBuffer.getShort(TCP_REQ_TRG_NUMB_INDEX).toInt()
                for (tmp in 0..(trgLen - 1)) {
                    val data = byteBuffer.getBit(tmp + TCP_REQ_TRG_DATA_INDEX * 8)
                    st.Ctrl.DBWrite(sid, cmd.toRegion(), trgAddr + tmp, data)
                }
            }
            else                   -> {
            }
        }

        // for Rsp Setup
        val trAddr: Int
        val trNbDt: Int
        val serial: Int

        if (cmd == MdCmdEnum.RD_BITS ||
            cmd == MdCmdEnum.RD_BITS_RO ||
            cmd == MdCmdEnum.RD_WORD_RO ||
            cmd == MdCmdEnum.RD_WORD)
        {
            trAddr  =  byteBuffer.getShort(TCP_REQ_TRG_ADDR_INDEX).toUInt()
            trNbDt  =  byteBuffer.getShort(TCP_REQ_TRG_NUMB_INDEX).toUInt()
            serial  =  byteBuffer.getShort(TCP_SERIAL_INDEX).toUInt()
        }else{
            trAddr  =  byteBuffer.getShort(TCP_REQ_TRG_ADDR_INDEX).toUInt()
            trNbDt  =  byteBuffer.getShort(TCP_REQ_TRG_NUMB_INDEX).toUInt()
            serial  =  byteBuffer.getShort(TCP_SERIAL_INDEX).toUInt()
        }
        return DealResponse(rsp = KM_TCP_Slave_Setup(st,sid,cmd,trAddr,trNbDt,serial))
    }else{
        if(st.IsSlave){
            return DealResponse(code = MdRspCode.MD_NOT_MASTER_ERROR)
        }
        //Response Frame
        val cmd = MdCmdEnum.parse(bus.RawBytes[TCP_STATION_NUMB_INDEX])
        if (cmd == MdCmdEnum.RD_BITS ||
            cmd == MdCmdEnum.RD_BITS_RO ||
            cmd == MdCmdEnum.RD_WORD_RO ||
            cmd == MdCmdEnum.RD_WORD)
        {
            bus.RawPkgLen = st.NowPoll.ShouldReturn
            //response data write to db
            val trgLen  = bus.RawBytes[RTU_RSP_BYTE_LEN_INDEX].toInt()
            val trgAddr = st.NowPoll.StartAddr
            if(cmd == MdCmdEnum.RD_WORD || cmd == MdCmdEnum.RD_WORD_RO){
                for (tmp in 0..(trgLen/2-1)){
                    val data = byteBuffer.getShort(tmp / 2 + TCP_RSP_DATA_INDEX)
                    st.Ctrl.DBWrite(sid, cmd.toRegion(), trgAddr+tmp, data)
                }
            }else{
                for (tmp in 0..(trgLen-1)){
                    val data = byteBuffer.getBit(tmp + TCP_RSP_DATA_INDEX * 8)
                    st.Ctrl.DBWrite(sid, cmd.toRegion(), trgAddr+tmp, data)
                }
            }
            return DealResponse()
        }else{
            //writing response needs nothing to do
            return DealResponse()
        }
    }
}

fun KM_TCP_Slave_Setup(st: Station, id:Int, cmd: MdCmdEnum, trAddr:Int, trNbDt:Int, serial:Int):ByteArray{

    val ret   = ByteArray(256)
    var index = 0
    if (cmd == MdCmdEnum.RD_WORD_RO ||
        cmd == MdCmdEnum.RD_WORD)
    {
        ret[index++] = serial.shr(8).toByte()
        ret[index++] = serial.and(0xff).toByte()
        ret[index++] = 0
        ret[index++] = 0
        val dataLen = TCP_RSP_RD_DATA_EXTRA_BYTE + trNbDt * 2
        ret[index++] = dataLen.shr(8).toByte()
        ret[index++] = dataLen.and(0xff).toByte()
        ret[index++] = id.toByte()
        ret[index++] = cmd.toCode().toByte()
        ret[index++] = (trNbDt * 2).toByte()
        for (tmp in 0..(trNbDt - 1)){
            val data = st.Ctrl.DBRead(id,cmd.toRegion(), trAddr + tmp)
            ret[index++] = data.shr(8).toByte()
            ret[index++] = data.and(0xff).toByte()
        }
        return ret.take(index).toByteArray()
    }


    if (cmd == MdCmdEnum.RD_BITS ||
        cmd == MdCmdEnum.RD_BITS_RO)
    {
        ret[index++] = serial.shr(8).toByte()
        ret[index++] = serial.and(0xff).toByte()
        ret[index++] = 0
        ret[index++] = 0
        val dataLen = TCP_RSP_RD_DATA_EXTRA_BYTE + (trNbDt + 7) / 8
        ret[index++] = dataLen.shr(8).toByte()
        ret[index++] = dataLen.and(0xff).toByte()
        ret[index++] = id.toByte()
        ret[index++] = cmd.toCode().toByte()
        ret[index++] = ((trNbDt + 7) / 8).toByte()

        ret[index]  = 0
        var tmpBit  = 0
        var tmpByte = 0

        for (tmp in 0..(trNbDt - 1)){
            val bit = st.Ctrl.DBRead(id,cmd.toRegion(), trAddr + tmp)
            if(bit>0){
                tmpByte =  (tmpByte or (0x01 shl tmpBit))
            }

            if (++tmpBit >= 8) {
                ret[index++] = tmpByte.toByte()
                ret[index]   = 0
                tmpByte      = 0
                tmpBit       = 0
            }
        }
        if(tmpBit!=0){
            ret[index++] = tmpByte.toByte()
        }
        return ret.take(index).toByteArray()
    }



    if (cmd == MdCmdEnum.WR_BITS_ONE ||
        cmd == MdCmdEnum.WR_WORD_ONE ||
        cmd == MdCmdEnum.WR_BITS_SOME ||
        cmd == MdCmdEnum.WR_WORD_SOME)
    {
        ret[index++] = serial.shr(8).toByte()
        ret[index++] = serial.and(0xff).toByte()
        ret[index++] = 0
        ret[index++] = 0
        ret[index++] = 0
        ret[index++] = TCP_RSP_REQ_SHORT_PKG_LEN.toByte()
        ret[index++] = id.toByte()
        ret[index++] = cmd.toCode().toByte()
        ret[index++] = (trAddr / 256).toByte()
        ret[index++] = (trAddr % 256).toByte()
        ret[index++] = (trNbDt / 256).toByte()
        ret[index++] = (trNbDt % 256).toByte()
        return ret.take(index).toByteArray()
    }
    return ByteArray(0)
}



fun KM_TCP_Master_Setup(bus: StationBus,poll: NowPolling):ByteArray{

    if(poll.polling == null || poll.station == null){
        return ByteArray(0)
    }
    val cmd = poll.polling!!.DevCmd
    val id      = poll.polling!!.StationID
    val trgAddr = poll.polling!!.StartAddr
    val trgNumb  = poll.polling!!.NumbTodo
    val serial  = bus.Serial
    val st  = poll.station!!
    if(bus.Serial++ > 0x7000){
        bus.Serial = 0
    }


    val ret = ByteArray(256)
    var index = 0
    if (cmd == MdCmdEnum.RD_WORD ||
        cmd == MdCmdEnum.RD_WORD_RO ||
        cmd == MdCmdEnum.RD_BITS_RO ||
        cmd == MdCmdEnum.RD_BITS)
    {
        ret[index++] = serial.shr(8).toByte()
        ret[index++] = serial.and(0xff).toByte()
        ret[index++] = 0
        ret[index++] = 0
        ret[index++] = 0
        ret[index++] = TCP_RSP_REQ_SHORT_PKG_LEN.toByte()
        ret[index++] = id.toByte()
        ret[index++] = cmd.toCode().toByte()
        ret[index++] = (trgAddr/256).toByte()
        ret[index++] = (trgAddr%256).toByte()
        ret[index++] = (trgNumb/256).toByte()
        ret[index++] = (trgNumb%256).toByte()
        return ret.take(index).toByteArray()
    }

    // _MODBUS_ANSWER_0X05
    // _MODBUS_ANSWER_0x06
    //单个写,长度都是8个,写位的时候1:0xFF00 0:0x0000
    if (cmd == MdCmdEnum.WR_BITS_ONE ||
        cmd == MdCmdEnum.WR_WORD_ONE)
    {
        ret[index++] = serial.shr(8).toByte()
        ret[index++] = serial.and(0xff).toByte()
        ret[index++] = 0
        ret[index++] = 0
        ret[index++] = 0
        ret[index++] = TCP_RSP_REQ_SHORT_PKG_LEN.toByte()
        ret[index++] = id.toByte()
        ret[index++] = cmd.toCode().toByte()
        ret[index++] = (trgAddr/256).toByte()
        ret[index++] = (trgAddr%256).toByte()
        val data = st.Ctrl.DBRead(id,cmd.toRegion(),trgAddr)
        if(cmd== MdCmdEnum.WR_BITS_ONE){
            if(data>0){
                ret[index++] = 0xFF.toByte()
                ret[index++] = 0
            }else{
                ret[index++] =  0
                ret[index++] =  0
            }
        }else{
            ret[index++] = (data/256).toByte()
            ret[index++] = (data%256).toByte()
        }
        return ret.take(index).toByteArray()
    }

    // _MODBUS_ANSWER_0x0F
    if (cmd == MdCmdEnum.WR_BITS_SOME)
    {
        ret[index++] = serial.shr(8).toByte()
        ret[index++] = serial.and(0xff).toByte()
        ret[index++] = 0
        ret[index++] = 0
        ret[index++] = 0
        ret[index++] = (TCP_REQ_WRP_EXTRA_BYTES+(trgNumb+7)/8).toByte()
        ret[index++] = id.toByte()
        ret[index++] = cmd.toCode().toByte()
        ret[index++] = (trgAddr/256).toByte()
        ret[index++] = (trgAddr%256).toByte()
        ret[index++] = (trgNumb/256).toByte()
        ret[index++] = (trgNumb%256).toByte()
        ret[index++] = (0+(trgNumb+7)/8).toByte()

        ret[index]  = 0
        var tmpBit  = 0
        var tmpByte = 0

        for (tmp in 0..(trgNumb-1)){
            val bit = st.Ctrl.DBRead(id,cmd.toRegion(),trgAddr+tmp)
            if(bit>0){
                tmpByte =  (tmpByte or (0x01 shl tmpBit))
            }
            if (++tmpBit >= 8) {
                ret[index++] = tmpByte.toByte()
                ret[index]   = 0
                tmpByte      = 0
                tmpBit       = 0
            }
        }
        if(tmpBit!=0){
            ret[index++] = tmpByte.toByte()
        }
        return ret.take(index).toByteArray()
    }


    if (cmd == MdCmdEnum.WR_WORD_SOME)
    {
        ret[index++] = serial.shr(8).toByte()
        ret[index++] = serial.and(0xff).toByte()
        ret[index++] = 0
        ret[index++] = 0
        ret[index++] = 0
        ret[index++] = (TCP_REQ_WRP_EXTRA_BYTES+trgNumb*2).toByte()
        ret[index++] = id.toByte()
        ret[index++] = cmd.toCode().toByte()
        ret[index++] = (trgAddr/256).toByte()
        ret[index++] = (trgAddr%256).toByte()
        ret[index++] = (trgNumb/256).toByte()
        ret[index++] = (trgNumb%256).toByte()
        ret[index++] = (0+trgNumb*2).toByte()

        for (tmp in 0..(trgNumb-1)){
            val data:Int = st.Ctrl.DBRead(id,cmd.toRegion(),trgAddr+tmp)
            ret[index++] = data.shr(8).toByte()
            ret[index++] = data.and(0xff).toByte()
        }
        return ret.take(index).toByteArray()
    }
    return ByteArray(0)
}









