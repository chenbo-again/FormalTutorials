package counter

import spinal.core.formal._
import spinal.core._
import spinal.lib._

class LimitedCounter extends Component {
  // The value register will always be between [2:10]
  val reset = slave Flow(UInt(4 bits))
  val workFlow = RegNext(reset) init(reset.getZero)

  val inc = in Bool()
  val value = Reg(UInt(4 bits)) init (2)
  when(inc && value < 10) {
    value := value + 1
  }
  when(workFlow.valid) {
    value := workFlow.payload
  }
}

class FormalCounterTester extends SpinalFormalFunSuite {
  def tester() {
    FormalConfig
      .withBMC(10)
      .withProve(10)
      .withCover(10)
      .withDebug
      .doVerify(new Component {
        val dut = FormalDut(new LimitedCounter())
        val inc = in Bool()
        dut.inc <> inc
        val flow = slave Flow(UInt(4 bits))
        dut.reset << flow
        val reset = ClockDomain.current.isResetActive
        assumeInitial(reset)

        val data = anyconst(UInt(4 bits))

        for(i <- 2 to 9) {
          cover(dut.value === i)
        }

        // assume(inc =/= flow.valid)
        assume(flow.payload >= 2 && flow.payload <= 10)

        val valueNotChange = dut.value =/= past(dut.value) + 1
        when(pastValid && past(inc) && !past(dut.workFlow.valid) && dut.value < 10){
          // assert(dut.value === past(dut.value) + 1)
          assert(!valueNotChange)
        }

        val hists = History(flow, 2, init = dut.reset.getZero)

        cover(pastValid && past(flow.valid) && past(flow.payload === data))
        when(pastValid && past(hists(1).valid) && past(hists(1).payload === data)) {
          assert(dut.value === data)
        }

        val outOfBound = dut.value > 10 || dut.value < 2
        assert(!outOfBound)
      })
  }
  test("formal_tester") {
    tester()
  }
}
