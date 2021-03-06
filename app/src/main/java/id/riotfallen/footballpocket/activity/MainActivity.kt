package id.riotfallen.footballpocket.activity

import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.view.MenuItem
import com.google.gson.Gson
import id.riotfallen.footballpocket.R
import id.riotfallen.footballpocket.adapter.recycler.home.HomeEventListAdapter
import id.riotfallen.footballpocket.adapter.recycler.home.HomeFavoriteEventListAdapter
import id.riotfallen.footballpocket.adapter.recycler.home.HomePlayerListAdapter
import id.riotfallen.footballpocket.api.ApiRepository
import id.riotfallen.footballpocket.db.database
import id.riotfallen.footballpocket.model.event.Event
import id.riotfallen.footballpocket.model.favorite.FavoriteEvent
import id.riotfallen.footballpocket.model.favorite.FavoriteTeam
import id.riotfallen.footballpocket.model.league.League
import id.riotfallen.footballpocket.model.player.Player
import id.riotfallen.footballpocket.model.team.Team
import id.riotfallen.footballpocket.presenter.EventsPresenter
import id.riotfallen.footballpocket.presenter.LeaguesPresenter
import id.riotfallen.footballpocket.presenter.PlayerPresenter
import id.riotfallen.footballpocket.presenter.TeamPresenter
import id.riotfallen.footballpocket.utils.PrefConfig
import id.riotfallen.footballpocket.utils.gone
import id.riotfallen.footballpocket.utils.invisible
import id.riotfallen.footballpocket.utils.visible
import id.riotfallen.footballpocket.view.EventView
import id.riotfallen.footballpocket.view.LeaguesView
import id.riotfallen.footballpocket.view.PlayersView
import id.riotfallen.footballpocket.view.TeamView
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.ctx
import org.jetbrains.anko.db.*
import org.jetbrains.anko.sdk25.coroutines.onClick
import org.jetbrains.anko.selector
import org.jetbrains.anko.startActivity

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener,
        EventView, LeaguesView, PlayersView, TeamView {

    private lateinit var actionBarDrawerToggle: ActionBarDrawerToggle
    private lateinit var leagueId: String
    private lateinit var teamId: String
    private lateinit var leaguePresenter: LeaguesPresenter
    private lateinit var playerPresenter: PlayerPresenter
    private lateinit var teamPresenter: TeamPresenter
    private lateinit var homeEventListAdapter: HomeEventListAdapter
    private lateinit var homePlayerListAdapter: HomePlayerListAdapter
    private lateinit var favoriteEventListAdapter: HomeFavoriteEventListAdapter

    private lateinit var request: ApiRepository
    private lateinit var gson: Gson

    private var favoriteEvents: MutableList<FavoriteEvent> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(mainActivityToolbar)
        supportActionBar?.setIcon(R.drawable.ic_brand)
        supportActionBar?.title = ""

        actionBarDrawerToggle = ActionBarDrawerToggle(this, mainActivityDrawerLayout,
                R.string.open, R.string.close)

        mainActivityNavigationView.itemIconTintList = null
        mainActivityNavigationView.setNavigationItemSelectedListener(this)
        mainActivityNavigationView.inflateMenu(R.menu.navigation_menu)

        mainActivityDrawerLayout.addDrawerListener(actionBarDrawerToggle)
        actionBarDrawerToggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        leagueId = PrefConfig(this).readIdLeague()
        teamId = PrefConfig(this).readIdTeam()

        request = ApiRepository()
        gson = Gson()

        leaguePresenter = LeaguesPresenter(this, request, gson)
        leaguePresenter.getDetailLeagues(leagueId)

        playerPresenter = PlayerPresenter(this, request, gson)
        playerPresenter.getTeamPlayers(teamId)

        teamPresenter = TeamPresenter(this, request, gson)
        teamPresenter.getTeamDetail(teamId)

        createLocalDb()
    }

    private fun createLocalDb() {
        database.use {
            createTable(FavoriteEvent.TABLE_FAVORITE_EVENT, true,
                    FavoriteEvent.ID to INTEGER + PRIMARY_KEY + AUTOINCREMENT,
                    FavoriteEvent.EVENT_ID to TEXT + UNIQUE,
                    FavoriteEvent.EVENT_DATE to TEXT,
                    FavoriteEvent.HOME_ID to TEXT,
                    FavoriteEvent.HOME_NAME to TEXT,
                    FavoriteEvent.HOME_SCORE to INTEGER,
                    FavoriteEvent.AWAY_ID to TEXT,
                    FavoriteEvent.AWAY_NAME to TEXT,
                    FavoriteEvent.AWAY_SCORE to INTEGER)
        }

        database.use {
            createTable(FavoriteTeam.TABLE_FAVORITE_TEAM, true,
                    FavoriteTeam.ID to INTEGER + PRIMARY_KEY + AUTOINCREMENT,
                    FavoriteTeam.TEAM_ID to TEXT + UNIQUE,
                    FavoriteTeam.TEAM_NAME to TEXT,
                    FavoriteTeam.TEAM_BADGE to TEXT)
        }

        loadFavoriteEvent()
    }

    private fun loadFavoriteEvent() {
        favoriteEvents.clear()
        database.use {
            val result = select(FavoriteEvent.TABLE_FAVORITE_EVENT)
            val favorite = result.parseList(classParser<FavoriteEvent>())
            favoriteEvents.addAll(favorite)
            if (favorite.isNotEmpty())
                showFavoriteEvent()
            else
                mainActivityRelativeLayoutFavorite.gone()
        }
    }

    private fun showFavoriteEvent() {
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        mainRecyclerViewFavoriteEvent.layoutManager = layoutManager
        mainRecyclerViewFavoriteEvent.itemAnimator = DefaultItemAnimator()
        favoriteEventListAdapter = HomeFavoriteEventListAdapter(this, favoriteEvents)
        mainRecyclerViewFavoriteEvent.adapter = favoriteEventListAdapter
        mainActivityRelativeLayoutFavorite.visible()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when(item.itemId){

            R.id.menu_team -> {
                startActivity<TeamActivity>()
            }

            R.id.menu_event -> {
                startActivity<EventActivity>()
            }
        }

        return false
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if(actionBarDrawerToggle.onOptionsItemSelected(item)){
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun showEventLoading() {
        mainProgressBarNextEvent.visible()
        mainRecyclerViewNextEvent.invisible()
    }

    override fun hideEventLoading() {
        mainProgressBarNextEvent.invisible()
        mainRecyclerViewNextEvent.visible()
    }

    override fun showEventData(data: MutableList<Event>) {
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        mainRecyclerViewNextEvent.layoutManager = layoutManager
        mainRecyclerViewNextEvent.itemAnimator = DefaultItemAnimator()
        homeEventListAdapter = HomeEventListAdapter(this, data)
        mainRecyclerViewNextEvent.adapter = homeEventListAdapter
    }

    override fun showPlayerData(data: List<Player>) {
        val layoutManager = GridLayoutManager(this, 3)
        mainRecyclerViewFavoritePlayer.layoutManager = layoutManager
        mainRecyclerViewFavoritePlayer.itemAnimator = DefaultItemAnimator()
        homePlayerListAdapter = HomePlayerListAdapter(this, data as MutableList<Player>)
        mainRecyclerViewFavoritePlayer.adapter = homePlayerListAdapter
        mainRecyclerViewFavoritePlayer.isNestedScrollingEnabled = false
    }

    override fun showLeagues(data: List<League>) {
        val leagues: MutableList<String> = arrayListOf()
        for (index in data.indices){
            if (index == 22){
                break
            }
            data[index].strLeague?.let { leagues.add(it) }
        }

        mainActivityLinearLayoutLeaguePicker.onClick {
            ctx.selector("Select your favorite football club", leagues) { _, i ->
                data[i].idLeague?.let { it1 -> PrefConfig(this@MainActivity).writeIdLeague(it1) }
                leagueId = PrefConfig(this@MainActivity).readIdLeague()
                leaguePresenter.getDetailLeagues(leagueId)
            }
        }
    }

    override fun showDetailLeague(data: List<League>) {
        mainActivityTextViewLeague.text = data[0].strLeague

        val eventPresenter = EventsPresenter(this, request, gson)
        eventPresenter.getNextEventList(leagueId)

        leaguePresenter.getLeagues()
    }

    override fun showTeamData(data: List<Team>) {
        mainActivityTextViewTeam.text = data[0].strTeam
        mainActivityLinearLayoutTeamPicker.setOnClickListener {
            startActivity<FavoriteTeamSelectorActivity>()
        }
    }


    override fun showPlayerLoading() {
        mainProgressBarFavoritePlayer.visible()
        mainRecyclerViewFavoritePlayer.invisible()
    }

    override fun hidePlayerLoading() {
        mainProgressBarFavoritePlayer.invisible()
        mainRecyclerViewFavoritePlayer.visible()
    }

    override fun showTeamLoading() {
    }

    override fun hideTeamLoading() {
    }

    override fun onResume() {
        super.onResume()

        teamId = PrefConfig(this).readIdTeam()
        playerPresenter.getTeamPlayers(teamId)
        teamPresenter.getTeamDetail(teamId)
        loadFavoriteEvent()
    }
}
