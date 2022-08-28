package mylib

import spinal.core._
import spinal.lib._
import spinal.core.formal._

object AsyncStreamForkFormal {
  def main(args: Array[String]) {
    FormalConfig
      .withBMC(20)
      .withProve(20)
      .withCover(20)
      // .withDebug
      .doVerify(new Component {
        val dut = FormalDut(new StreamFork(UInt(8 bits), 2, false))
        val reset = ClockDomain.current.isResetActive

        assumeInitial(reset)
        val input = slave(Stream(UInt(8 bits)))
        val output_0 = master(Stream(UInt(8 bits)))
        val output_1 = master(Stream(UInt(8 bits)))


        input >> dut.io.input
        output_0 << dut.io.outputs(0)
        output_1 << dut.io.outputs(1)
        
        // when(reset || past(reset)) {
        //   assume(input.valid === False)
        // }

        // 1. fork, 两个得到的结果应该一致等于 input
        when(output_0.fire) {
          assert(output_0.payload === input.payload)
        }

        when(output_1.fire) {
          assert(output_1.payload === input.payload)
        }

        // 2. 无阻塞
        cover(output_0.fire || !output_1.fire)
        cover(!output_0.fire || output_1.fire)

        // 3. Stream 语义
        dut.io.input.withAssumes()
        dut.io.outputs(0).withAsserts()
        dut.io.outputs(1).withAsserts()

      })

  }

}
