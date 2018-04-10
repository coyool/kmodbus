package KModbus

import Base.getBit
import Base.toUInt
import Base.toUnsigned
import java.nio.ByteBuffer


fun IsThisRequire(raw: ByteArray, slave: Boolean): Boolean {
    val cmd = MdCmdEnum.parse(raw[RTU_PKG_CMD_INDEX])
    when(cmd) {
        MdCmdEnum.RD_BITS,
        MdCmdEnum.RD_BITS_RO,
        MdCmdEnum.RD_WORD_RO,
        MdCmdEnum.RD_WORD      -> {
            if (CrcCheck(raw, RTU_SHORT_CMD_LEN)) {
                return true
            }
        }
        MdCmdEnum.WR_WORD_ONE,
        MdCmdEnum.WR_BITS_ONE  -> {
            if (CrcCheck(raw, RTU_SHORT_CMD_LEN)) {
                return slave
            }
        }
        MdCmdEnum.WR_BITS_SOME,
        MdCmdEnum.WR_WORD_SOME -> {
            if (CrcCheck(raw, (raw[RTU_REQ_WRP_BYTE_LEN_INDEX].toUnsigned() + RTU_REQ_WRP_EXTRA_BYTE))) {
                return true
            }
        }
        else                   -> {
        }
    }
    return false
}




fun MD_RTU_Data_Deal(bus: StationBus): DealResponse {
    val sid        = bus.RawBytes[RTU_STATION_NUMB_INDEX].toInt()
    val byteBuffer = ByteBuffer.wrap(bus.RawBytes)
    //1,IDCheck
    val st2find:Station? = bus.Stations.find { it.Ctrl.IDCheck(sid) } ?: return DealResponse(code = MdRspCode.MD_DEV_ADDR_ERR)

    val st = st2find!!
//    if(!st.Ctrl.IDCheck(sid)){
//        return DealResponse(code = MdRspCode.MD_DEV_ADDR_ERR)
//    }

    //2,Cmd Check
    if(bus.RawBytes[RTU_PKG_CMD_INDEX].toInt() !in MdSupportCmd){
        return DealResponse(code = MdRspCode.MD_CMD_ERR)
    }
    //3,CrcCheck
    if(!CrcCheck(bus.RawBytes,bus.RawPkgLen)){
        return DealResponse(code = MdRspCode.MD_CRC_ERR)
    }

    //4,check if is a require
    if(IsThisRequire(bus.RawBytes, st.IsSlave)){
        val cmd     = MdCmdEnum.parse(bus.RawBytes[RTU_PKG_CMD_INDEX])
        val trgAddr: Int
        val trgLen:  Int
        when(cmd){
            MdCmdEnum.WR_WORD_ONE  ->{
                val tmp = byteBuffer.getShort(RTU_REQ_SINGLE_DATA_INDEX)
                trgAddr = byteBuffer.getShort(RTU_TRG_ADDR_INDEX).toUInt()
                st.Ctrl.DBWrite(sid,cmd.toRegion(),trgAddr,tmp)
            }
            MdCmdEnum.WR_WORD_SOME -> {
                trgAddr = byteBuffer.getShort(RTU_TRG_ADDR_INDEX).toUInt()
                trgLen  = byteBuffer.getShort(RTU_REQ_WRP_TRG_NUMB_INDEX).toUInt()
                for (tmp in 0..(trgLen - 1)) {
                    val data = byteBuffer.getShort(tmp * 2 + RTU_REQ_WRP_TRG_DATA_INDEX)
                    st.Ctrl.DBWrite(sid, cmd.toRegion(), trgAddr+tmp, data)
                }
            }
            MdCmdEnum.WR_BITS_ONE  ->{
                trgAddr = byteBuffer.getShort(RTU_TRG_ADDR_INDEX).toInt()
                val tmp = byteBuffer.get(RTU_REQ_SINGLE_DATA_INDEX).toUnsigned()
                st.Ctrl.DBWrite(sid,cmd.toRegion(),trgAddr,tmp)
            }
            MdCmdEnum.WR_BITS_SOME ->{
                trgAddr = byteBuffer.getShort(RTU_TRG_ADDR_INDEX).toInt()
                trgLen  = byteBuffer.getShort(RTU_REQ_WRP_TRG_NUMB_INDEX).toInt()
                for (tmp in 0..(trgLen - 1)) {
                    val data = byteBuffer.getBit(tmp  + RTU_REQ_WRP_TRG_DATA_INDEX* 8)
                    st.Ctrl.DBWrite(sid, cmd.toRegion(), trgAddr+tmp, data)
                }
            }
            else                   -> {
            }
        }

        // for Rsp Setup
        val par1: Int
        val par2: Int

        if (cmd == MdCmdEnum.RD_BITS ||
            cmd == MdCmdEnum.RD_BITS_RO ||
            cmd == MdCmdEnum.RD_WORD_RO ||
            cmd == MdCmdEnum.RD_WORD)
        {
            par1  =  byteBuffer.getShort(RTU_TRG_ADDR_INDEX).toUInt()
            par2  =  byteBuffer.getShort(RTU_REQ_RD_TRG_NUMB_INDEX).toUInt()
        }else{
            par1  =  byteBuffer.getShort(RTU_TRG_ADDR_INDEX).toUInt()
            par2  =  byteBuffer.getShort(RTU_REQ_SINGLE_DATA_INDEX).toUInt()
        }
        return DealResponse(rsp = MD_RTU_Slave_Setup(st,sid,cmd,par1,par2))
    }else{
        if(st.IsSlave){
            return DealResponse(code = MdRspCode.MD_NOT_MASTER_ERROR)
        }
        //Response Frame
        val cmd = MdCmdEnum.parse(bus.RawBytes[RTU_PKG_CMD_INDEX])
        if (cmd == MdCmdEnum.RD_BITS ||
            cmd == MdCmdEnum.RD_BITS_RO ||
            cmd == MdCmdEnum.RD_WORD_RO ||
            cmd == MdCmdEnum.RD_WORD)
        {
            if(st.NowPoll.ShouldReturn != bus.RawPkgLen){
                if(!CrcCheck(bus.RawBytes,st.NowPoll.ShouldReturn)){
                    return DealResponse(code = MdRspCode.MD_ERR_RSP_LEN_ERR)
                }
                bus.RawPkgLen = st.NowPoll.ShouldReturn
            }

            //response data write to db
            val trgLen  = bus.RawBytes[RTU_RSP_BYTE_LEN_INDEX].toInt()
            val trgAddr = st.NowPoll.StartAddr
            if(cmd == MdCmdEnum.RD_WORD || cmd == MdCmdEnum.RD_WORD_RO){
                for (tmp in 0..(trgLen/2-1)){
                    val data = byteBuffer.getShort(tmp / 2 + RTU_RSP_RD_DATA_INDEX)
                    st.Ctrl.DBWrite(sid, cmd.toRegion(), trgAddr+tmp, data)
                }
            }else{
                for (tmp in 0..(trgLen-1)){
                    val data = byteBuffer.getBit(tmp + RTU_RSP_RD_DATA_INDEX * 8)
                    st.Ctrl.DBWrite(sid, cmd.toRegion(), trgAddr+tmp, data)
                }
            }
            return DealResponse()
        }else{
            //writing response needs nothing to do
            return DealResponse()
        }
    }

//    return DealResponse(code = MdRspCode.MD_NULL_ERROR)
}

fun MD_RTU_Slave_Setup(st: Station, id:Int, cmd: MdCmdEnum, par01:Int, par02:Int):ByteArray{

    val ret   = ByteArray(256)
    var index = 0
    if (cmd == MdCmdEnum.RD_WORD_RO ||
        cmd == MdCmdEnum.RD_WORD)
    {
        ret[index++] = id.toByte()
        ret[index++] = cmd.toCode().toByte()
        ret[index++] = (par02 * 2).toByte()
        for (tmp in 0..(par02 - 1)){
            val data = st.Ctrl.DBRead(id,cmd.toRegion(), par01 + tmp)
            ret[index++] = data.shr(8).toByte()
            ret[index++] = data.and(0xff).toByte()
        }
        val crc = GetCrc(ret,0,index)
        ret[index++] = crc[0].toByte()
        ret[index++] = crc[1].toByte()
        return ret.take(index).toByteArray()
    }


    if (cmd == MdCmdEnum.RD_BITS ||
        cmd == MdCmdEnum.RD_BITS_RO)
    {
        ret[index++] = id.toByte()
        ret[index++] = cmd.toCode().toByte()
        ret[index++] = ((par02 + 7) / 8).toByte()

        ret[index]  = 0
        var tmpBit  = 0
        var tmpByte = 0

        for (tmp in 0..(par02 - 1)){
            val bit = st.Ctrl.DBRead(id,cmd.toRegion(), par01 + tmp)
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

        val crc = GetCrc(ret,0,index)
        ret[index++] = crc[0].toByte()
        ret[index++] = crc[1].toByte()
        return ret.take(index).toByteArray()
    }



    if (cmd == MdCmdEnum.WR_BITS_ONE ||
        cmd == MdCmdEnum.WR_WORD_ONE ||
        cmd == MdCmdEnum.WR_BITS_SOME ||
        cmd == MdCmdEnum.WR_WORD_SOME)
    {
        ret[index++] = id.toByte()
        ret[index++] = cmd.toCode().toByte()
        ret[index++] = (par01 / 256).toByte()
        ret[index++] = (par01 % 256).toByte()
        ret[index++] = (par02 / 256).toByte()
        ret[index++] = (par02 % 256).toByte()
        val crc = GetCrc(ret,0,index)
        ret[index++] = crc[0].toByte()
        ret[index++] = crc[1].toByte()
        return ret.take(index).toByteArray()
    }
    return ByteArray(0)
}



fun KM_RTU_Master_Setup(st: Station, id:Int, cmd: MdCmdEnum, trgAddr:Int, trgLen:Int):ByteArray{
    val ret = ByteArray(256)
    var index = 0
    // _MODBUS_ANSWER_0X01
    // _MODBUS_ANSWER_0x02
    // _MODBUS_ANSWER_0x03
    // _MODBUS_ANSWER_0x04
    if (cmd == MdCmdEnum.RD_WORD ||
        cmd == MdCmdEnum.RD_WORD_RO ||
        cmd == MdCmdEnum.RD_BITS_RO ||
        cmd == MdCmdEnum.RD_BITS)
    {
        ret[index++] = id.toByte()
        ret[index++] = cmd.toCode().toByte()
        ret[index++] = (trgAddr/256).toByte()
        ret[index++] = (trgAddr%256).toByte()
        ret[index++] = (trgLen/256).toByte()
        ret[index++] = (trgLen%256).toByte()
        val crc = GetCrc(ret,0,index)
        ret[index++] = crc[0].toByte()
        ret[index++] = crc[1].toByte()
        return ret.take(index).toByteArray()
    }

    // _MODBUS_ANSWER_0X05
    // _MODBUS_ANSWER_0x06
    //单个写,长度都是8个,写位的时候1:0xFF00 0:0x0000
    if (cmd == MdCmdEnum.WR_BITS_ONE ||
        cmd == MdCmdEnum.WR_WORD_ONE)
    {

        ret[index++] = id.toByte()
        ret[index++] = cmd.toCode().toByte()
        ret[index++] = (trgAddr/256).toByte()
        ret[index++] = (trgAddr%256).toByte()
        val data = st.Ctrl.DBRead(id,cmd.toRegion(),trgAddr)
        if(cmd== MdCmdEnum.WR_BITS_ONE){
            if(data>0){
                ret[index++] = 0xFF.toByte()
                ret[index++] = 0x00.toByte()
            }else{
                ret[index++] =  0
                ret[index++] =  0
            }
        }else{
            ret[index++] = (data/256).toByte()
            ret[index++] = (data%256).toByte()
        }
        val crc = GetCrc(ret,0,index)
        ret[index++] = crc[0].toByte()
        ret[index++] = crc[1].toByte()
        return ret.take(index).toByteArray()
    }

    // _MODBUS_ANSWER_0x0F
    if (cmd == MdCmdEnum.WR_BITS_SOME)
    {
        ret[index++] = id.toByte()
        ret[index++] = cmd.toCode().toByte()
        ret[index++] = (trgAddr/256).toByte()
        ret[index++] = (trgAddr%256).toByte()
        ret[index++] = (trgLen/256).toByte()
        ret[index++] = (trgLen%256).toByte()
        ret[index++] = ((trgLen + 7)/8).toByte()

        ret[index]  = 0
        var tmpBit  = 0
        var tmpByte = 0

        for (tmp in 0..(trgLen-1)){
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

        val crc = GetCrc(ret,0,index)
        ret[index++] = crc[0].toByte()
        ret[index++] = crc[1].toByte()
        return ret.take(index).toByteArray()
    }


    if (cmd == MdCmdEnum.WR_WORD_SOME)
    {
        ret[index++] = id.toByte()
        ret[index++] = cmd.toCode().toByte()
        ret[index++] = (trgAddr/256).toByte()
        ret[index++] = (trgAddr%256).toByte()
        ret[index++] = (trgLen/256).toByte()
        ret[index++] = (trgLen%256).toByte()
        ret[index++] = (trgLen*2%256).toByte()

        for (tmp in 0..(trgLen-1)){
            val data:Int = st.Ctrl.DBRead(id,cmd.toRegion(),trgAddr+tmp)
            ret[index++] = data.shr(8).toByte()
            ret[index++] = data.and(0xff).toByte()
        }
        val crc = GetCrc(ret,0,index)
        ret[index++] = crc[0].toByte()
        ret[index++] = crc[1].toByte()
        return ret.take(index).toByteArray()
    }

    return ByteArray(0)
}









