package io.muun.apollo.presentation.ui.fragments.home

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.TextView
import butterknife.BindView
import butterknife.OnClick
import com.airbnb.lottie.LottieAnimationView
import com.skydoves.balloon.*
import icepick.State
import io.muun.apollo.R
import io.muun.apollo.domain.model.CurrencyDisplayMode
import io.muun.apollo.domain.model.Operation
import io.muun.apollo.domain.selector.UtxoSetStateSelector
import io.muun.apollo.presentation.ui.base.SingleFragment
import io.muun.apollo.presentation.ui.utils.StyledStringRes
import io.muun.apollo.presentation.ui.utils.getDrawable
import io.muun.apollo.presentation.ui.view.BalanceView
import io.muun.apollo.presentation.ui.view.MuunHomeCard
import io.muun.apollo.presentation.ui.view.NewOpBadge
import io.muun.common.utils.BitcoinUtils
import kotlin.math.abs


class HomeFragment : SingleFragment<HomePresenter>(), HomeView {

    @BindView(R.id.chevron)
    lateinit var chevron: LottieAnimationView

    @BindView(R.id.chevron_container)
    lateinit var chevronContainer: View

    @BindView(R.id.new_op_badge)
    lateinit var newOpBadge: NewOpBadge

    @BindView(R.id.home_balance_view)
    lateinit var balanceView: BalanceView

    @BindView(R.id.home_security_center_card)
    lateinit var securityCenterCard: MuunHomeCard

    @State
    @JvmField
    var utxoSetState: UtxoSetStateSelector.UtxoSetState? = null

    var balloon: Balloon? = null

    override fun inject() {
        component.inject(this)
    }

    override fun getLayoutResource() =
        R.layout.fragment_home

    override fun initializeUi(view: View?) {
        super.initializeUi(view)

        parentActivity.header.apply {
            visibility = View.VISIBLE
            showTitle(R.string.app_name)
            setElevated(false)
        }

        securityCenterCard.let {
            it.icon = getDrawable(R.drawable.ic_lock)
            it.body = StyledStringRes(requireContext(), R.string.home_security_center_card_body)
                .toCharSequence()
        }

        securityCenterCard.setOnClickListener {
            presenter.navigateToSecurityCenter()
        }

        // The detector detect the fling gesture for the chevron
        // The first detects the fling in the horizontal area around the chevron.
        // The second detects the fling in the chevron itself.
        // Having both allows us to retain the default on click (and thus animation) for the chevron

        val containerDetector = GestureDetector(
                requireContext(), GestureListener(chevron, requireContext(), true)
        )
        chevronContainer.setOnTouchListener { _, event -> containerDetector.onTouchEvent(event) }

        val chevronDetector = GestureDetector(
                requireContext(), GestureListener(chevron, requireContext(), false)
        )
        chevron.setOnTouchListener { _, event -> chevronDetector.onTouchEvent(event) }

    }

    class GestureListener(val chevron: View, val context: Context, val down: Boolean):
            GestureDetector.SimpleOnGestureListener() {

        private val minVelocity = ViewConfiguration.get(context).scaledMinimumFlingVelocity
        private val minTravelDistance = ViewConfiguration.get(context).scaledTouchSlop

        override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent?,
                velocityX: Float,
                velocityY: Float
        ): Boolean {
            checkNotNull(e1)
            checkNotNull(e2)

            // Future reader: for MotionEvent we want rawX/Y, other coordinates suck (BIG TIME)
            // See: https://stackoverflow.com/q/1410885/901465
            if (abs(velocityY) > minVelocity && e1.rawY - e2.rawY > minTravelDistance) {
                chevron.performClick()
                return true
            }

            return super.onFling(e1, e2, velocityX, velocityY)
        }

        override fun onDown(e: MotionEvent?): Boolean {
            // onDown determines whether the detector intercepts the event or it keeps looking
            // for someone to handle it. If true, all events are handled by the detector.
            return down
        }

    }

    override fun onPause() {
        super.onPause()

        chevron.removeAllLottieOnCompositionLoadedListener()
        balloon?.dismiss()
        balloon = null
        lockManager.setUnlockListener(null)
    }

    override fun showTooltip() {
        if (balloon != null) {
            return
        }

        if (lockManager.isLockSet) {
            lockManager.setUnlockListener {
                showTooltip()
            }
            return;
        }

        this.balloon = createBalloon(parentActivity) {
            setLayout(R.layout.view_new_home_tooltip)
            setArrowSize(15)
            setArrowOrientation(ArrowOrientation.BOTTOM)
            setArrowConstraints(ArrowConstraints.ALIGN_ANCHOR)
            setArrowPosition(0.5f)
            setWidthRatio(1.0f)
            setMarginRight(53)
            setMarginLeft(53)
            setCornerRadius(4f)
            setBackgroundColorResource(R.color.new_home_tooltip)
            setOnBalloonClickListener(OnBalloonClickListener { onOperationHistoryChevronClick() })
            setBalloonAnimation(BalloonAnimation.NONE)
            setDismissWhenTouchOutside(false)
            setLifecycleOwner(this@HomeFragment)
        }

        chevron.addLottieOnCompositionLoadedListener {
            // Turns out the balloon library has a few known limitations regarding multi line text
            // layout. If we set max width, they seem to solve themselves. But, we can't set a fixed
            // value in the xml so we do this lovely thing here.
            // https://github.com/skydoves/Balloon/issues/55
            val textView = balloon!!.getContentView().findViewById<TextView>(R.id.new_home_tooltip_text)
            textView.maxWidth = requireView().width - 56 * 2

            balloon!!.showAlignTop(chevron)
        }
    }

    override fun setBalance(homeBalanceState: HomePresenter.HomeBalanceState) {

        balanceView.setBalance(homeBalanceState)

        if (utxoSetState != homeBalanceState.utxoSetState) {
            when (homeBalanceState.utxoSetState) {
                UtxoSetStateSelector.UtxoSetState.PENDING -> chevron.setAnimation(R.raw.lm_chevron_pending)
                UtxoSetStateSelector.UtxoSetState.RBF -> chevron.setAnimation(R.raw.lm_chevron_rbf)
                UtxoSetStateSelector.UtxoSetState.CONFIRMED -> chevron.setAnimation(R.raw.lm_chevron_regular)
            }
            utxoSetState = homeBalanceState.utxoSetState
        }
    }

    override fun setNewOp(newOp: Operation, mode: CurrencyDisplayMode) {

        var amountInBtc = BitcoinUtils.satoshisToBitcoins(newOp.amount.inSatoshis)

        val animRes: Int
        when {
            newOp.isOutgoing -> {
                amountInBtc = amountInBtc.negate()
                animRes = R.anim.new_op_badge_outgoing

            }
            newOp.isCyclical -> {
                animRes = R.anim.new_op_badge_outgoing
                amountInBtc = BitcoinUtils.satoshisToBitcoins(newOp.fee.inSatoshis).negate()

            }
            else -> {
                animRes = R.anim.new_op_badge_incoming
            }
        }

        newOpBadge.setAmount(amountInBtc, mode)

        newOpBadge.startAnimation(animRes)
    }

    override fun setUserRecoverable(recoverable: Boolean) {
        if (recoverable) {
            securityCenterCard.visibility = View.GONE

        } else {
            securityCenterCard.visibility = View.VISIBLE
        }
    }

    @OnClick(R.id.home_balance_view)
    fun onBalanceClick() {
        val hidden = balanceView.toggleVisibility()
        presenter.setBalanceHidden(hidden)
    }

    @OnClick(R.id.home_receive_button)
    fun onReceiveClick() {
        presenter.navigateToReceiveScreen()
    }

    @OnClick(R.id.home_send_button)
    fun onSendClick() {
        presenter.navigateToSendScreen()
    }

    @OnClick(R.id.chevron)
    fun onOperationHistoryChevronClick() {
        presenter.navigateToOperations()
    }
}