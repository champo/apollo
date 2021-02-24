package io.muun.apollo.domain.action.session

import io.muun.apollo.data.external.Globals
import io.muun.apollo.data.logging.LoggingContext
import io.muun.apollo.data.net.HoustonClient
import io.muun.apollo.domain.action.LogoutActions
import io.muun.apollo.domain.action.base.BaseAsyncAction1
import io.muun.apollo.domain.action.fcm.GetFcmTokenAction
import io.muun.common.model.CreateSessionOk
import rx.Observable
import javax.inject.Inject
import javax.inject.Singleton
import javax.validation.constraints.NotNull

@Singleton
class CreateLoginSessionAction @Inject constructor(
    private val houstonClient: HoustonClient,
    private val getFcmToken: GetFcmTokenAction,
    private val logoutActions: LogoutActions,

): BaseAsyncAction1<String, CreateSessionOk>() {

    override fun action(email: String): Observable<CreateSessionOk> =
        Observable.defer { createSession(email) }

    /**
     * Creates a new session to log into Houston, associated with a given email.
     */
    private fun createSession(@NotNull email: String): Observable<CreateSessionOk> {

        logoutActions.destroyWalletToStartClean()

        return getFcmToken.action()
            .flatMap { fcmToken ->
                houstonClient.createLoginSession(
                    Globals.INSTANCE.oldBuildType,
                    Globals.INSTANCE.versionCode,
                    fcmToken,
                    email
                )
            }
            .doOnNext { LoggingContext.configure(email, "NotLoggedYet") }
    }
}
