# kmodbus
English :
1, 

Chinese
1, 这是一个Modbus程序库, 但它并不关联任何的收发机制。 用户输入一个数组ByteArray, 程序将返回一个ByteArray。用户不必要关心处理过程。
2, 处理过程中,本模块会调用已经设定的读写函数(DbWrite和DbRead函数)完成数据处理或者其他任何操作


## How to use. ##
#### Initial a station pack. ####
```
//PD == Package Deal
class PDCtrls1 : ModbusCtrls {
    override fun IDCheck(id: Int): Boolean {
        return id == 1
    }

    override fun DBRead(id: Int, dbType: MdRegion, addr: Int): Int {
        if(dbType == MdRegion.MD_REGION_1XXXX || dbType == MdRegion.MD_REGION_2XXXX){
            return addr and 0x01
        }
        return dbType.toCode() * 10000 + id * 1000 + addr
    }

    override fun DBWrite(id: Int, dbType: MdRegion, addr: Int, data: Short): Boolean {
//        println("W:\t$id\t@\t$addr -> $data")
        globalMapOfValue.put(addr,data.toUInt())
        return true
    }

    override fun HeaderFind(buffer: ByteBuffer): Boolean {
        return true
    }
}


class PDCtrls2 : ModbusCtrls {
    private val stations = listOf(2,4,6,8,10)
    override fun IDCheck(id: Int): Boolean {
        return id in stations //5 stations of the same type
    }

    override fun DBRead(id: Int, dbType: MdRegion, addr: Int): Int {
        if(dbType == MdRegion.MD_REGION_1XXXX || dbType == MdRegion.MD_REGION_2XXXX){
            return addr and 0x01
        }
        return dbType.toCode() * 10000 + id * 1000 + addr
    }

    override fun DBWrite(id: Int, dbType: MdRegion, addr: Int, data: Short): Boolean {
        globalMapOfValue.put(addr,data.toUInt())
        return true
    }

    override fun HeaderFind(buffer: ByteBuffer): Boolean {
        return true
    }
}

val PDStation1 = Station(
        IsSlave = false,
        Ctrl = PDCtrls1())

val PDStation2 = Station(
        IsSlave = true,
        Ctrl = PDCtrls2())
```

#### PrePare the ByteArray to be done ####
All the info is organized by StationBus
```
        val test_station_id = 10
        val test_start_addr = 0x1123
        val test_numb_todo  = 12
        val PDBus = StationBus(
                RawPkgLen = 0,
                RawBytes  = ByteArray(256),
                RspPkgLen = 0,
                Stations  = listOf(PDStation1, PDStation2)
        )
        val polling = Polling(
                StationID  = test_station_id,
                StartAddr  = test_start_addr,
                NumbTodo   = test_numb_todo,
                DevCmd     = MdCmdEnum.RD_BITS
        )
	
        PDBus.Serial          = 0x3344
        PDBus.NowPoll.polling = polling
        PDBus.NowPoll.station = PDStation1
```
or
```

        PDBus.RawBytes = KM_TCP_Master_Setup(
                PDBus,PDBus.NowPoll
        )		
        PDBus.RawPkgLen = PDBus.RawBytes.size
```
or
```
        PDBus.RawBytes =  SocketIORead()/SerialComRead()
        PDBus.RawPkgLen = PDBus.RawBytes.size
```



#### Call the main fuctions for data deal ####
```
	val ret = KM_TCP_Data_Deal(PDBus)
	ret.code shouldBe MdRspCode.MD_ERR_OK
	ret.rsp.toSimpleHex() shouldBe "3344000000050A01025505"
	for (tmp in 0..(test_numb_todo - 1)){
		ret.rsp.getBit(tmp + TCP_RSP_DATA_INDEX*8) shouldBe
				PDStation1.Ctrl.DBRead(test_station_id,MdRegion.MD_REGION_1XXXX, test_start_addr + tmp)
	}
```


