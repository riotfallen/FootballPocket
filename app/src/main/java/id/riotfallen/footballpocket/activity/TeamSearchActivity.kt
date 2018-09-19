package id.riotfallen.footballpocket.activity

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.Menu
import android.view.MenuItem
import android.widget.SearchView
import com.google.gson.Gson
import id.riotfallen.footballpocket.R
import id.riotfallen.footballpocket.adapter.recycler.team.TeamsAdapter
import id.riotfallen.footballpocket.api.ApiRepository
import id.riotfallen.footballpocket.model.team.Team
import id.riotfallen.footballpocket.presenter.TeamPresenter
import id.riotfallen.footballpocket.utils.invisible
import id.riotfallen.footballpocket.utils.visible
import id.riotfallen.footballpocket.view.TeamView
import kotlinx.android.synthetic.main.activity_team_search.*

class TeamSearchActivity : AppCompatActivity(), TeamView {


    private lateinit var teamPresenter: TeamPresenter
    private lateinit var teamsAdapter: TeamsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_team_search)
        setSupportActionBar(teamSearchToolbar)
        supportActionBar?.title = "Search Team"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val gson = Gson()
        val apiRepository = ApiRepository()

        teamPresenter = TeamPresenter(this, apiRepository, gson)
    }

    override fun showTeamLoading() {
        teamSearchProgressBar.visible()
        teamSearchRecyclerView.invisible()
    }

    override fun hideTeamLoading() {
        teamSearchProgressBar.invisible()
        teamSearchRecyclerView.visible()
    }

    override fun showTeamData(data: List<Team>) {
        val filter: MutableList<Team> = mutableListOf()
        for (index in data.indices) {
            if (data[index].strSport.equals("Soccer")) {
                filter.add(data[index])
            }
        }

        val layoutManager: RecyclerView.LayoutManager = GridLayoutManager(this, 1)
        teamSearchRecyclerView.layoutManager = layoutManager
        teamSearchRecyclerView.itemAnimator = DefaultItemAnimator()
        teamsAdapter = TeamsAdapter(this, filter)
        teamSearchRecyclerView.adapter = teamsAdapter
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_search, menu)
        val searchItem = menu?.findItem(R.id.menu_search)

        val searchView: SearchView? = searchItem?.actionView as SearchView

        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                if (!query.isEmpty()) {
                    teamPresenter.searchTeam(query)
                }
                return false
            }

            override fun onQueryTextChange(query: String): Boolean {
                if (!query.isEmpty()) {
                    teamPresenter.searchTeam(query)
                }
                return false
            }
        })
        searchView?.requestFocus()
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }
}
