package hu.lifeos.app

import android.app.Application
import android.content.Context
import hu.lifeos.app.data.AppDatabase
import hu.lifeos.app.data.PhoneCalendar
import hu.lifeos.app.data.SettingsStore
import hu.lifeos.app.food.FoodContent
import hu.lifeos.app.food.FoodRepository
import hu.lifeos.app.satori.SatoriContent
import hu.lifeos.app.satori.SatoriRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Az app közös szolgáltatásai; minden képernyő innen éri el az adatokat. */
class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val db: AppDatabase = AppDatabase.create(context)
    val settings = SettingsStore(context)
    val phoneCalendar = PhoneCalendar(context)
    val satori = SatoriRepository(db, SatoriContent.load(context), scope)
    val food = FoodRepository(db, FoodContent.load(context), scope)
}

class LifeApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.scope.launch { container.satori.load(); container.food.load() }
    }
}

val Context.container: AppContainer get() = (applicationContext as LifeApp).container
