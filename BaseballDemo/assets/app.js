define("appkit/adapter",
  [],
  function() {
    "use strict";
    var Adapter = DS.Adapter.extend({
      /** Start the ball rolling by 'finding' a table.  We ignore the 'type' and just look at the 'id' as the table name
       */
      findQuery: function(store, type, opts, array) {
        throw new Error('findQuery not yet supported');
      },

      find: function(store, type, id) {
        throw new Error('FindById not supported - use watch()');
      },

      // I'm not sure that I really want to support this behavior
      // But at least it's not one at a time
      createRecords: function(store, type, set) {
        throw new Error('Create not yet supported');
      },

      createRecord: function() {
        throw new Error('Create not yet supported');
      },

      updateRecord: function() {
        throw new Error('Ziggrid does not support updating');
      },

      deleteRecord: function() {
        throw new Error('Ziggrid does not support deleting');
      },

      toString: function() {
        return 'Ziggrid.Adapter';
      }
    });



    return Adapter;
  });
define("appkit/app",
  ["appkit/utils/percentage_of_data","resolver","appkit/store","appkit/ziggrid/demux","appkit/models/player","appkit/initializers/watcher","appkit/initializers/csv","appkit/initializers/connection_manager","appkit/components/bean-production","appkit/components/bean-leaderboard","appkit/components/bean-homeruns"],
  function(__dependency1__, Resolver, Store, demux, Player, watcherInitializer, csvInitializer, connectionManagerInitializer, BeanProduct, BeanLeaderboard, BeanHomeruns) {
    "use strict";
    var precision = __dependency1__.precision;

    // Enable late registration helpers/components
    Ember.FEATURES['container-renderables'] = true;

    var App = Ember.Application.extend({
      modulePrefix: 'appkit', // TODO: loaded via config
      Store: Store,
      Resolver: Resolver,
      gameDateRanges: {
        '2006': [92, 274],
        '2007': [91, 274],
        '2008': [85, 274],
        '2009': [95, 279],
        '2010': [94, 276],
        '2011': [90, 271],
        '2012': [88, 277]
      }
    });

    App.initializer(watcherInitializer);
    App.initializer(csvInitializer);
    App.initializer(connectionManagerInitializer);

    App = App.create();
    App.deferReadiness(); // defering to allow sync boot with Ziggrid

    function round(val) {
      if (!isNaN(val) && !/^\d+$/.test(val)) {
        return val.toFixed(3);
      } else {
        return (val || val === 0) ? val : 'N/A';
      }
    }

    App.register('helper:round', Ember.Handlebars.makeBoundHelper(round));

    App.register('helper:name-from-code', Ember.Handlebars.makeBoundHelper(function(code) {
      return Player.nameFromCode(code) || code;
    }));

    App.register('helper:precision', Ember.Handlebars.makeBoundHelper(function(value, p) {
      return precision(value, p);
    }));

    App.register('helper:quadrant-value', Ember.Handlebars.makeBoundHelper(function(value) {
      //value = (value && value > 0 && value < 3) ? value : -1; //Math.random() * 0.3 + 0.3;
      return round(value);
    }));



    // TODO: happier way to do this automatically?
    // This way is bad because the component subclasses don't
    // get their injections...
    Ember.Handlebars.helper('bean-production', BeanProduct);
    Ember.Handlebars.helper('bean-leaderboard', BeanLeaderboard);
    Ember.Handlebars.helper('bean-homeruns', BeanHomeruns);

    // For our range input in bean-player
    Ember.TextField.reopen({
      attributeBindings: ['step', 'min', 'max']
    });


    return App;
  });
define("appkit/components/bean-homeruns",
  ["appkit/components/bean-table"],
  function(BeanTable) {
    "use strict";

    var Homeruns = BeanTable.extend({
      type: 'Leaderboard_homeruns_groupedBy_season',
      entryType: 'LeaderboardEntry_homeruns_groupedBy_season',

      headers: ['Home Runs', 'HR']
    });



    return Homeruns;
  });
define("appkit/components/bean-leaderboard",
  ["appkit/components/bean-table"],
  function(BeanTable) {
    "use strict";

    var Leaderboard = BeanTable.extend({
      type: 'Leaderboard_average_groupedBy_season',
      entryType: 'LeaderboardEntry_average_groupedBy_season',

      headers: ['Batting Average', 'AVG']
    });



    return Leaderboard;
  });
define("appkit/components/bean-player-profile",
  ["appkit/ziggrid/demux"],
  function(demux) {
    "use strict";

    var PlayerProfile = Ember.Component.extend({
      player: null,
      init: function () {
        this.connectionManager = this.container.lookup('connection_manager:main'); // inject
        this._super();
      },
      players: function() {
        var Player = this.container.lookupFactory('model:player');
        var allStars = Ember.get(Player, 'allStars');

        // just build for real Players the first time
        // this list doesn't change so we don't care
        // also the CP caches.
        return Ember.keys(allStars).map(function(entry) {
          return allStars[entry];
        }).map(function(entry) {
          return Player.create(entry);
        });
      }.property(),

      playerWillChange: function() {
        var oldPlayer = this.get('player');
        if (oldPlayer) {
          this.unwatchProfile();
        }
      }.observesBefore('player'),

      playerChanged: function() {
        var newPlayer = this.get('player');
        if (newPlayer) {
          this.watchProfile();
        }
        this.set('imageFailedToLoad', false);
      }.observes('player').on('init'),

      watchHandle: null,
      profile: null,

      watchProfile: function() {
        var handle = ++demux.lastId;

        this.set('watchHandle', handle);
        this.set('profile', null);

        var player = this;
        demux[handle] = {
          update: function(data) {
            player.set('profile', data);
          }
        };

        var query = {
          watch: 'Profile',
          unique: handle,
          player: this.get('player.code')
        };

        // Send the JSON message to the server to begin observing.
        var stringified = JSON.stringify(query);
        this.connectionManager.send(stringified);
      },

      unwatchProfile: function() {
        var watchHandle = this.get('watchHandle');

        if (!watchHandle) {
          throw new Error('No handle to unwatch');
        }

        this.connectionManager.send(JSON.stringify({
          unwatch: watchHandle
        }));

        this.set('watchHandle', null); // clear handle
      },

      // TODO: combine the various player car
      imageFailedToLoad: false,
      imageUrl: function() {

        if (this.get('imageFailedToLoad')) {
          return '/players/404.png';
        }

        var code = this.get('player.code');
        if (!code) { return; }
        return '/players/' + code + '.png';
      }.property('player.code', 'imageFailedToLoad').readOnly(),

      listenForImageLoadingErrors: function() {
        var component = this;

        this.$('img').error(function() {
          Ember.run(component, 'set', 'imageFailedToLoad', true);
        });
      }.on('didInsertElement')
    });


    return PlayerProfile;
  });
define("appkit/components/bean-player",
  [],
  function() {
    "use strict";
    var Player = Ember.Component.extend({
      progress: 0,
      isPlaying: false,
      progressText: null,
      showNub: true,
      _nubProgress: 0,
      nubProgressIsSynced: true,

      generator: Ember.computed.alias('connectionManager.generator'),

      progressTextStyle: function() {
        var nubProgress = this.get('nubProgress') || 0;
        return 'left: ' + (nubProgress * 99 + 0) + '%;';
      }.property('nubProgress'),

      nubProgress: function(key, val) {
        if (arguments.length === 2) {
          // Setter. Gets called when user grabs the nub.
          if (this.get('progress') - val < 0.01) {
            this.set('nubProgressIsSynced', true);
            Ember.run.next(this, 'notifyPropertyChange', 'nubProgress');
          } else {
            this.set('nubProgressIsSynced', false);
            this.set('_nubProgress', val);
          }
        } else {
          // Getter
          if (this.get('nubProgressIsSynced')) {
            return this.get('progress');
          } else {
            return this.get('_nubProgress');
          }
        }
      }.property('progress', 'nubProgressIsSynced'),

      progressPercentage: function() {
        return this.get('progress') * 100;
      }.property('progress'),

      progressBarStyle: function() {
        return 'width: ' + this.get('progressPercentage') + '%;';
      }.property('progress'),

      actions: {
        play: function() {
          if (this.get('isPlaying')) { return; }
          this.get('generator').start();
          this.set('isPlaying', true);
          this.sendAction('didBeginPlaying');
        },

        pause: function() {
          if (!this.get('isPlaying')) { return; }
          this.get('generator').stop();
          this.set('isPlaying', false);
          this.sendAction('didEndPlaying');
        }
      }
    });


    return Player;
  });
define("appkit/components/bean-production",
  ["appkit/components/bean-table"],
  function(BeanTable) {
    "use strict";

    var Production = BeanTable.extend({
      type: 'Leaderboard_production_groupedBy_season',
      entryType: 'LeaderboardEntry_production_groupedBy_season',

      headers: ['Production', 'PR']
    });



    return Production;
  });
define("appkit/components/bean-quadrant",
  [],
  function() {
    "use strict";
    var w = 690,
        h = 500,
        radius = 5;

    function playerX(scale) {
      return function(player) {
        return scale(Ember.get(player, 'goodness')) + 'px';
      };
    }

    function playerY(scale) {
      return function(player) {
        return scale(Ember.get(player, 'hotness')) + 'px';
      };
    }

    function get(path) {
      return function(object) {
        return Ember.get(object, path);
      };
    }

    function appendPlayers(players, component) {
      players.
        append('span').
          classed('name', true).
          text(get('PlayerName'));

      players.
        append('div').
        classed('circle', true);

      players.
        on('click', function(d, i) {
          clickPlayer.call(this, d, component);
      });
    }

    function clickPlayer(playerData, component) {
      d3.select('.selected').classed('selected', false);
      var selectedPlayer = component.get('selectedPlayer');

      var selected = d3.select(this);

      if (selectedPlayer === playerData) {
        deselect(component);
      } else {
        component.set('selectedPlayer', playerData);
        selected.classed('selected', true);
      }
    }

    function deselect(component) {
      component.set('selectedPlayer', null);
      d3.select('.selected').classed('selected', false);
    }

    var Quadrant = Ember.Component.extend({
      selectedPlayer: null,
      renderGraph: function() {
        var $container = this.$().find('.quadrant-container');
        createSVG($container.get(0));

        this.$popup = this.$().find('.quadrant-popup');
        this.xscale = d3.scale.linear().
          domain([0, 1]).
          range([9.5, w-9.5]).
          clamp(true);

        this.yscale = d3.scale.linear().
          domain([0, 1]).
          range([h-9.5, 9.5]).
          clamp(true);

        this.dataDidChange();

        // TODO: make sure we clean this guy up
        (function syncPopupPosition(){
          var selected = $('.selected');
          var popup = $('.quadrant-popup');

          popup.css({
            left: selected.css('left'),
            top: selected.css('top')
          });

          window.requestAnimationFrame(syncPopupPosition);
        }());

      }.on('didInsertElement'),

      renderD3: function() {
        var season = this.get('season');

        var container = d3.select(this.$('.quadrant-graph')[0]);
        var data = this.get('players').filter(function(player) {
          return player.hasSeason(season);
        }).filterBy('realized');

        var component = this;

        var xscale = this.xscale;
        var yscale = this.yscale;

        var players = container.
          selectAll('.quadrant-player').
          data(data, get('name'));

        players.exit().each(fadeOutPlayer).
          transition().
          duration(300).
          style({
            opacity: 0
          }).remove();

        players.enter().
          append('div').
          attr('data-id', get('name')).
          attr('data-name', get('humanizedName')).
          classed('quadrant-player', true).
          style({
            opacity: 0,
            left: playerX(xscale),
            top: playerY(yscale)
          }).call(function(players) {
            players.
              append('span').
                classed('name', true).
                text(get('humanizedName'));

            players.
              append('div').
              classed('circle', true);

            players.
              on('click', function(d, i) {
                clickPlayer.call(this, d, component);
              });
          });

        players.transition().
          duration(1000).
          ease('linear').
          style({
            opacity: 1,
            left: playerX(xscale),
            top: playerY(yscale)
          });

        function fadeOutPlayer() {
          if (d3.select(this).classed('selected')) {
            deselect(component);
          }
        }
      },

      dataDidChange: function() {
        Ember.run.throttle(this, 'renderD3', 100);
      }.observes('players.@each.hotness', 'players.@each.goodness', 'season'),

      teardownGraph: function() {
        // TODO: what kind of teardown does d3 need?
      }.on('willDestroyElement'),

      click: function(e) {
        if (e.target.tagName !== 'rect') { return; }
        deselect(this);
      }
    });

    function createSVG(parentElement) {
      var svg = d3.select(parentElement).append('svg:svg')
          .attr('width', w)
          .attr('height', h);

      // gradient
      var defs = svg.append('svg:defs');

      var backgroundLinearGradient = defs.append('svg:linearGradient').
        attr('id', 'background-linear-gradient').
        attr('x1', '0%').
        attr('y1', '100%').
        attr('x2', '100%').
        attr('y2', '0%');

      backgroundLinearGradient.append('svg:stop').
          attr('offset', '20%').
          attr('stop-color', '#0A4D65').
          attr('stop-opacity', 1);

      backgroundLinearGradient.append('svg:stop').
          attr('offset', '80%').
          attr('stop-color', '#8D470B').
          attr('stop-opacity', 1);

      var backgroundRadialGradient = defs.append('svg:radialGradient').
        attr('id', 'background-radial-gradient').
        attr('cx', '50%').
        attr('cy', '50%').
        attr('r',  '50%').
        attr('fx', '50%').
        attr('fy', '50%');

      backgroundRadialGradient.append('svg:stop').
          attr('offset', '0%').
          attr('stop-color', 'black').
          attr('stop-opacity', 0.8);

      backgroundRadialGradient.append('svg:stop').
          attr('offset', '100%').
          attr('stop-opacity', 0);

      svg.append('svg:rect').
          attr('width', w).
          attr('height', h).
          style('fill', 'url(#background-linear-gradient)');

      svg.append('svg:rect').
          attr('width', w).
          attr('height', h).
          style('fill', 'url(#background-radial-gradient)');
      // \gradient
      //
      svg.append('line').
        attr('stroke-dasharray', '2 2').
        attr('stroke-width', 0.3).
        attr('stroke', 'rgba(255, 255, 255, 0.52)').
        attr('x1', w/2).
        attr('y1', 0).
        attr('x2', w/2).
        attr('y2', h);

      svg.append('line').
        attr('stroke-dasharray', '2 2').
        attr('stroke-width', 0.3).
        attr('stroke', 'rgba(255, 255, 255, 0.52)').
        attr('y1', h/2).
        attr('x1', 0).
        attr('y2', h/2).
        attr('x2', w);
    }


    return Quadrant;
  });
define("appkit/components/bean-standings",
  [],
  function() {
    "use strict";
    var get = Ember.get;

    var Table = Ember.Component.extend({

      // Template args
      league: null,
      region: null,

      title: Ember.computed.alias('region.name'),

      headers: function() {
        return [this.get('title'), 'W', 'L'];
      }.property()
    });



    return Table;
  });
define("appkit/components/bean-table",
  ["appkit/ziggrid/demux"],
  function(demux) {
    "use strict";

    var Table = Ember.Component.extend({

      // e.g. 'Leaderboard_production_groupedBy_season'
      type: null,

      // e.g. 'LeaderboardEntry_production_groupedBy_season'
      entryType: null,

      // unique ID assigned by Watcher
      handle: null,

      // Have to specify this so that subclasses get it too.
      templateName: 'components/bean-table',

      season: null,

      headers: null,

      entries: function() {
        // TODO: more efficient way to display just the first N elements?
        return this.get('content').slice(0, 5);
      }.property('content.[]'),

      content: [],

      startWatching: function() {
        var watcher = this.container.lookup('watcher:main'),
            handle = this.get('handle');

        if (handle) {
          watcher.unwatch(handle);
        }

        var model = watcher.watch(this.get('type'),
                                  this.get('entryType'),
                                  { season: '' + this.get('season') });

        this.set('content', model.get('table'));

        // this is kinda hacky/brittle; this is updated in watcher.watch()
        this.set('handle', demux.lastId);
      }.observes('season').on('init')
    });



    return Table;
  });
define("appkit/components/bean-team-standing",
  ["appkit/ziggrid/demux"],
  function(demux) {
    "use strict";

    var TeamStanding = Ember.Component.extend({
      _applicationController: Ember.computed(function(){
        return this.container.lookup('controller:application');
      }),

      season: Ember.computed.alias('_applicationController.season'),
      team: null,
      handle: null,
      watcher: Ember.computed(function() {
        return this.container.lookup('watcher:main');
      }),

      winLoss: function() {
        var watcher = this.get('watcher');
        var code = this.get('team.watchCode');
        var season = this.get('season');

        var handle = this.get('handle');

        if (handle) {
          // unsubscribe;
          watcher.unwatch(handle);
        }

        var subscription = {
          team: code,
          season: '' + season
        };

        console.log('watching', subscription);

        var model = watcher.watch('WinLoss', 'WinLoss', subscription);

        this.set('handle', demux.lastId);

        return model;
      }.property('team.code', 'season'),
      wins: Ember.computed.alias('winLoss.wins'),
      losses: Ember.computed.alias('winLoss.losses')
    });


    return TeamStanding;
  });
define("appkit/controllers/application",
  [],
  function() {
    "use strict";
    var ApplicationController = Ember.Controller.extend({

      years: [2007, 2008, 2009, 2010, 2011, 2012],

      season: null,

      currentPage: 0,

      // TODO: move to ApplicationView when it works
      pageContainerStyle: function() {
        var currentPage = this.get('currentPage');
        return "margin-left: " + (-960 * currentPage) +  "px;";
      }.property('currentPage'),

      actions: {
        togglePage: function() {
          this.set('currentPage', +!this.get('currentPage'));
        }
      }
    });


    return ApplicationController;
  });
define("appkit/controllers/filter",
  [],
  function() {
    "use strict";
    var FilterController = Ember.Controller.extend({
      needs: ['application'],

      years: Ember.computed.sort('controllers.application.years', function(a, b){
        return (a > b) ? -1 : 1;
      }),

      selectedFilter: null
    });


    return FilterController;
  });
define("appkit/controllers/filter_item",
  [],
  function() {
    "use strict";
    var FilterItemController = Ember.ObjectController.extend({
      isSelected: function() {
        return this.get('content') === this.get('parentController.selectedFilter');
      }.property('parentController.selectedFilter')
    });


    return FilterItemController;
  });
define("appkit/controllers/quadrant",
  ["appkit/utils/percentage_of_data"],
  function(__dependency1__) {
    "use strict";
    var percentageOfData = __dependency1__.percentageOfData;

    var get = Ember.get;

    var QuadrantController = Ember.Controller.extend({
      needs: ['filter', 'application'],
      filter: Ember.computed.alias('controllers.filter.selectedFilter'),
      showing: Ember.computed.bool('controllers.application.currentPage'),

      progress: function() {
        var gameDate = this.get('currentDate');

        if (!gameDate) {
          return 0;
        }

        return percentageOfData(gameDate.day, parseInt(gameDate.season, 10));
      }.property('currentDate'),

      currentDate: Ember.computed.alias('gameDates.lastObject'),
      //currentDate: { season: '2009', day: '180' },

      currentDateText: function() {
        var date = this.get('currentDate');
        if (!date) { return; }

        var season = get(date, 'season');
        return moment('' + season).day(parseInt(get(date, 'day'), 10) + 1).format('MMMM D, YYYY');
      }.property('currentDate')
    });


    return QuadrantController;
  });
define("appkit/controllers/quadrant_player",
  [],
  function() {
    "use strict";
    var QuadrantPlayerController = Ember.ObjectController.extend({

      playerWillChange: function() {
        var oldPlayer = this.get('content');
        if (oldPlayer) {
          oldPlayer.unwatchProfile();
        }
      }.observesBefore('content'),

      playerChanged: function() {
        var newPlayer = this.get('content');
        if (newPlayer) {
          newPlayer.watchProfile();
        }
      }.observes('content')
    });



    return QuadrantPlayerController;
  });
define("appkit/controllers/standings",
  ["appkit/ziggrid/demux","appkit/utils/group_by"],
  function(demux, groupBy) {
    "use strict";

    function Region(name, teams) {
      this.name = name;
      this.teams = teams;
    }

    function League(name, teams) {
      var grouped = groupBy('Division', teams);
      this.regions = [
        new Region('East',    grouped.East),
        new Region('Central', grouped.Central),
        new Region('West',    grouped.West)
      ];

      this.name = name;
    }

    var StandingsController = Ember.Controller.extend({
      needs: ['application'],
      season: Ember.computed.alias('controllers.application.season'),
      leagues: Ember.computed(function(){
        var TeamListing = this.container.lookupFactory('model:team_listing');
        var byLeague = TeamListing.allTeamsByLeague;

        return [
          new League('American League', byLeague.AL),
          new League('National League', byLeague.NL)
        ];
      }),
      // unique ID assigned by Watcher
      handle: null,

      startWatching: function() {
      }.observes('season').on('init')
    });


    return StandingsController;
  });
define("appkit/flags",
  [],
  function() {
    "use strict";
    var flags = {
      LOG_WEBSOCKETS: true
    };


    return flags;
  });
define("appkit/initializers/connection_manager",
  ["appkit/ziggrid/connection_manager"],
  function(ConnectionManager) {
    "use strict";
    var url = '/ziggrid/';


    var initializer = {
      name: 'connection-manager',
      before: 'registerComponents',
      initialize: function(container, application) {
        var connectionManager = ConnectionManager.create({
          url: url,
          namespace: application
        });

        application.register('connection_manager:main', connectionManager, {
          instantiate: false
        });

        application.inject('component:bean-player',
                   'connectionManager',
                   'connection_manager:main');
      }
    };


    return initializer;
  });
define("appkit/initializers/csv",
  ["appkit/utils/index_by","appkit/utils/group_by","appkit/utils/aggregate_players","appkit/utils/csv"],
  function(indexBy, groupBy, aggregatePlayers, csv) {
    "use strict";

    // Load all the csv data
    var initializer = {
      name: 'load-csv',
      initialize: function(container, application) {
        application.deferReadiness();

        Ember.RSVP.hash({
          allStars: csv('all-stars.csv'),
          allTeams: csv('all-teams.csv'),
          allPlayers: csv('all-players.csv')
        }).then(function(hash) {
          application.advanceReadiness();

          var Player = container.lookupFactory('model:player');
          var TeamListing = container.lookupFactory('model:team_listing');

          // TODO: store object would be better
          Player.reopenClass({
            dataByName: indexBy('PlayerCode', hash.allPlayers),
            allStars: aggregatePlayers(hash.allStars)
          });

          TeamListing.reopenClass({
            allTeamsByLeague: groupBy('League', hash.allTeams),
            allTeams: hash.allTeams
          });

        }).fail(Ember.RSVP.rethrow);
      }
    };


    return initializer;
  });
define("appkit/initializers/watcher",
  ["appkit/ziggrid/watcher"],
  function(Watcher) {
    "use strict";

    var initializer = {
      name: 'ziggrid-watcher',
      initialize: function(container, application) {
        var watcher = new Watcher(application);
        application.register('watcher:main', watcher, { instantiate: false });
      }
    };


    return initializer;
  });
define("appkit/models/player",
  [],
  function() {
    "use strict";
    var Player = Ember.Object.extend({
      PlayerName: function(){
        var code = this.get('code');
        var playerData = Player.dataByName[code];
        var name;

        if (playerData) {
          name = playerData.PlayerName;
        }

        return name || this.get('name') || code;
      }.property('code')
    });

    Player.reopenClass({
      getPlayerData: function(code) {
        throw new Error('implement me');
      },

      nameFromCode: function(code) {
        var player = this.dataByName[code];

        return player && player.PlayerName;
      },

      data: undefined,
      playerCodes: function() {
        return Ember.keys(this.allStars);
      }
    });


    return Player;
  });
define("appkit/models/quadrant_player",
  ["appkit/ziggrid/demux","appkit/models/player","appkit/flags"],
  function(demux, Player, flags) {
    "use strict";

    var App = window.App;

    var QuadrantPlayer = Ember.Object.extend({
      init: function(){
        this._super();
        QuadrantPlayer.all.pushObject(this);
        QuadrantPlayer.allByCode[this.get('code')] = this;
      },

      code: Ember.computed.alias('data.code'),

      realized: false,
      hotness: 0,
      goodness: 0,
      watchHandle: null,
      imageUrl: function(){
        return '/players/' + this.get('code') + '.png';
      }.property('data.name').readOnly(),
      watching: Ember.computed.bool('watchHandle'),

      // the actual player data resides on the Player mode,
      // this merely decorates. It is possible for us to have
      // inconsitent data, has this extra abstractoin
      data: function() {
        var name = this.get('name');
        var data = Player.allStars[name] || {};
        var playerData = Player.dataByName[name] || {};
        Ember.merge(data, playerData);
        return data;
      }.property('name'),

      hasSeason: function(season) {
        var seasons = this.get('data.seasons');

        return !!(seasons && seasons[season]);
      },

      humanizedName: Ember.computed.oneWay('data.PlayerName'),

      watchProfile: function() {
        // TODO: inject ziggrid:connection-manager
        var connectionManager = getConnectionManager();

        var handle = ++demux.lastId;

        this.set('watchHandle', handle);
        this.set('profile', null);

        var player = this;

        demux[handle] = {
          update: function(data) {
            player.set('profile', data);
          }
        };

        var query = {
          watch: 'Profile',
          unique: handle,
          player: this.get('name')
        };

        // Send the JSON message to the server to begin observing.
        var stringified = JSON.stringify(query);
        connectionManager.send(stringified);
      },

      unwatchProfile: function() {
        // TODO: inject ziggrid:connection-manager
        var connectionManager = getConnectionManager();
        var watchHandle = this.get('watchHandle');

        if (!watchHandle) {
          throw new Error('No handle to unwatch');
        }

        connectionManager.send(JSON.stringify({
          unwatch: watchHandle
        }));

        this.set('watchHandle', null); // clear handle
      }
    });

    QuadrantPlayer.reopenClass({
      all: [],
      allByCode: {},
      findOrCreateByName: function(playerName) {
        var player = QuadrantPlayer.all.findProperty('name', playerName);

        if (!player) {
          player = QuadrantPlayer.create({
            name: playerName
          });
        }

        return player;
      },
      watchPlayers: function(playerNames, season, dayOfYear) {

        playerNames.forEach(function(playerName, i) {
          watchAttribute('Snapshot_playerSeasonToDate',
                         playerName,
                         season,
                         dayOfYear);

          watchAttribute('Snapshot_clutchnessSeasonToDate',
                         playerName,
                         season,
                         dayOfYear);


          QuadrantPlayer.findOrCreateByName(playerName);
        });

        return QuadrantPlayer.all; // TODO: some record array.
      }
    });

    function updateQuadrantPlayer(data) {
      if (flags.LOG_WEBSOCKETS) {
        console.log('updateQuadrantPlayer', data);
      }

      var attrs = {
        realized: true
      };

      var player = QuadrantPlayer.allByCode[data.player];

      if (data.average) {
        attrs.goodness = normalizedQuadrantValue(player, 'hotness', data.average);
      }

      if (data.correlation) {
        attrs.hotness = normalizedQuadrantValue(player, 'goodness', data.correlation);
      }

      if (player) {
        player.setProperties(attrs);
      } else {
        attrs.name = data.player;
        QuadrantPlayer.create(attrs);
      }
    }

    function normalizedQuadrantValue(player, key, value) {
      if (isValidQuadrantValue(value)) {
        return value;
      } else {
        return (player && Ember.get(player, key)) || Math.random();
      }
    }

    function isValidQuadrantValue(value) {
      return value && value >= 0 && value <= 1;
    }

    function watchAttribute(type, playerName, season, dayOfYear) {

      var handle = ++demux.lastId;
      demux[handle] = {
        update: updateQuadrantPlayer
      };

      var hash = {
        watch: type,
        unique: handle,
        player: playerName//,
        //season: season
      };

      if (dayOfYear) {
        hash.dayOfYear = dayOfYear;
      }

      // Send the JSON message to the server to begin observing.
      var stringified = JSON.stringify(hash);
      getConnectionManager().send(stringified);

      // fireStubbedData(handle, playerName, 500 + i*500);
    }

    // TODO: inject
    function getConnectionManager() {
      return App.__container__.lookup('connection_manager:main');
    }


    return QuadrantPlayer;
  });
define("appkit/models/team_listing",
  [],
  function() {
    "use strict";
    var TeamListing = Ember.Object.extend({
    });

    TeamListing.reopenClass({
      all: []
    });


    return TeamListing;
  });
define("appkit/routes/application",
  ["appkit/models/player","appkit/models/quadrant_player"],
  function(Player, QuadrantPlayer) {
    "use strict";

    var season = 2007;

    var ApplicationRoute = Ember.Route.extend({
      setupController: function(controller) {

        var watcher = this.container.lookup('watcher:main');
        this.watcher = watcher;

        var gameDates = watcher.watchGameDate();

        controller.set('season', season);

        this.controllerFor('quadrant').set('gameDates', gameDates);

        this.updateQuadrantPlayers(season);
      },

      updateQuadrantPlayers: function(filter) {

        var filterController = this.controllerFor('filter');
        filterController.set('selectedFilter', filter);

        // TODO: grab this dynamically from leaderboard.

        var allStarPlayerCodes = this.container.lookupFactory('model:player').playerCodes();

        var players = QuadrantPlayer.watchPlayers(allStarPlayerCodes);
        this.controllerFor('quadrant').set('players', players);
      },

      actions: {
        selectFilter: function(filter) {
          this.updateQuadrantPlayers(filter);
        },
        didBeginPlaying: function() {
          this.watcher.keepSendingGameDates = true;
        },
        didEndPlaying: function() {
          this.watcher.keepSendingGameDates = false;
        }
      }
    });


    return ApplicationRoute;
  });
define("appkit/store",
  ["appkit/adapter"],
  function(Adapter) {
    "use strict";

    var Store = DS.Store.extend({
      adapter: Adapter
    });


    return Store;
  });
define("appkit/utils/aggregate_players",
  [],
  function() {
    "use strict";
    function aggregatePlayers(players) {
      var result = { };

      players.forEach(function(entry) {
        var code = entry.PlayerCode;
        var player = result[code] = result[code] || {
          name: entry.PlayerName,
          code: code,
          seasons: {}
        };

        player.seasons[entry.Year] = entry;
      });

      return result;
    }


    return aggregatePlayers;
  });
define("appkit/utils/csv",
  [],
  function() {
    "use strict";

    return Ember.RSVP.denodeify(d3.csv);
  });
define("appkit/utils/group_by",
  [],
  function() {
    "use strict";
    var get = Ember.get;

    function groupBy(property, collection) {
      var index = {};

      collection.forEach(function(entry) {
        var key = get(entry, property);
        index[key] = index[key] || [];
        index[key].push(entry);
      });

      return index;
    }


    return groupBy;
  });
define("appkit/utils/index_by",
  [],
  function() {
    "use strict";
    function indexBy(property, collection) {
      var index = {};

      collection.forEach(function(entry) {
        index[Ember.get(entry, property)] = entry;
      });

      return index;
    }


    return indexBy;
  });
define("appkit/utils/percentage_of_data",
  ["exports"],
  function(__exports__) {
    "use strict";
    var ranges = {
      2007: [ 91, 274 ],
      2008: [ 85, 274 ],
      2009: [ 95, 279 ],
      2010: [ 94, 276 ],
      2011: [ 90, 271 ],
      2012: [ 88, 277 ]
    };

    var seasons = Object.keys(ranges).map(Number);

    function precision(n, p) {
      return Math.floor(n * p) / p;
    }

    function normalizeDay(dayOfYear, season) {
      var range = ranges[season];

      if (!range) { throw new Error('Unknown Season: ' + season); }

      var start = range[0];
      var end = range[1];

      var totalGameDaysInSeason = end - start;
      var normalizedGameDay = dayOfYear - start;

      return normalizedGameDay;
    }

    function percentageOfSeason(dayOfYear, season) {
      var range = ranges[season];

      if (!range) { throw new Error('Unknown Season: ' + season); }

      var start = range[0];
      var end = range[1];

      var totalGameDaysInSeason = end - start;
      var normalizedGameDay = normalizeDay(dayOfYear, season);

      return precision(normalizedGameDay / totalGameDaysInSeason, 100);
    }

    function percentageOfData(dayOfYear, season) {
      var range = ranges[season];
      if (!range) { throw new Error('Unknown Season: ' + season); }

      var index = seasons.indexOf(season);
      var normalizedGameDay = normalizeDay(dayOfYear, season);

      var proportion = ((index * 180) + normalizedGameDay) / 1108;
      return precision(proportion, 100);
    }


    __exports__.percentageOfSeason = percentageOfSeason;
    __exports__.percentageOfData = percentageOfData;
    __exports__.precision = precision;
  });
define("appkit/views/filter",
  [],
  function() {
    "use strict";
    var FilterView = Ember.View.extend({
      elementId: 'filter-view'
    });


    return FilterView;
  });
define("appkit/ziggrid/connection_manager",
  ["appkit/ziggrid/generator","appkit/ziggrid/observer","appkit/ziggrid/demux","appkit/flags"],
  function(Generator, Observer, demux, flags) {
    "use strict";

    var ConnectionManager = Ember.Object.extend({

      url: null,

      // Reference to the global app namespace where we'll be installing
      // dynamically generated DS.Model classes
      namespace: null,

      generator: null,

      establishConnection: function() {

        var self = this;

        this.observers = {};
        this.initNeeded = 1;
        this.initCompleted = 0;

        var messages = [];

        var conn = this.conn = jQuery.atmosphere.subscribe({
          url: this.url + 'updates',
          transport: 'websocket',
          fallbackTransport: 'long-polling',

          // handle the 'open' message
          onOpen: function(response) {
            conn.push(JSON.stringify({ action: 'init' }));
          },

          // and then handle each incoming message
          onMessage: function(msg) {
            // Have to clone because jQuery atmosphere reuses response objects.
            messages.push({
              status: msg.status,
              responseBody: msg.responseBody
            });
            Ember.run.throttle(self, 'flushMessages', messages, 150);
          }
        });
      }.on('init'),

      flushMessages: function(messages) {
        while (messages.length) {
          var message = messages.shift();
          this.handleMessage(message);
        }
      },

      handleMessage: function(msg) {
        if (msg.status === 200) {

          if (flags.LOG_WEBSOCKETS) {
            console.log('Received message ' + msg.responseBody);
          }

          var body = JSON.parse(msg.responseBody);

          if (body['deliveryFor']) {
          console.log("Should not be here");
            // TODO: shouldn't this be in observer.js?
            var h = demux[body['deliveryFor']];
            if (h && h.update) {
              if (body.payload.table) {
                // Assume tabular data
                h.update(body.payload.table);
              } else {
                h.update(body.payload);
              }
            }
          } else if (body['error']) {
            console.error(body['error']);
          } else if (body['modelName']) {
            this.registerModel(body.modelName, body.model);
          } else if (body['server']) {
            var endpoint = body.endpoint,
            addr = 'http://' + endpoint + '/ziggrid/',
            server = body.server;

            if (flags.LOG_WEBSOCKETS) {
              console.log('Have new ' + server + ' server at ' + endpoint);
            }
            this.registerServer(server, addr);

          } else if (body['status']) {
            var stat = body['status'];
            if (stat === 'initdone') {
              this.initDone();
            } else {
              console.log('Do not recognize ' + stat);
            }
          } else
            console.log('could not understand ' + msg.responseBody);
        } else {
          console.log('HTTP Error:', msg.status);
          //if (callback && callback.error)
          //callback.error('HTTP Error: ' + msg.status);
        }
      },

      registerModel: function(name, model) {
        var attrs = {};
        for (var p in model) {
          if (!model.hasOwnProperty(p)) { continue; }

          var type = model[p];
          if (type.rel === 'attr') {
            attrs[p] = DS.attr(type.name);
          } else if (type.rel === 'hasMany') {
            attrs[p] = DS.hasMany('App.' + type.name.capitalize());
          } else {
            console.log('Unknown type:', type);
          }
        }

        var newClass = DS.Model.extend(attrs);
        newClass.reopenClass({
          model: model
        });

        this.namespace[name] = newClass;
      },

      registerServer: function(server, addr) {
        var self = this;
        if (server === 'generator') {
          this.set('generator', Generator.create(addr));
        } else if (server === 'ziggrid') {
          //return;
          if (!this.observers[addr]) {
            this.initNeeded++;

            this.observers[addr] = Observer.create(addr, function(newConn) {
              self.observers[addr] = newConn;
              self.initDone();
            });
          }
        }
      },

      initDone: function() {
        if (++this.initCompleted === this.initNeeded) {
          window.App.advanceReadiness();
        }
      },

      send: function(msg) {
        var observers = this.observers;

        if (flags.LOG_WEBSOCKETS) {
          console.log('sending ', msg, 'to', observers);
        }

        for (var u in observers) {
          if (observers.hasOwnProperty(u)) {
            observers[u].push(msg);
          }
        }
      }
    });


    return ConnectionManager;
  });
define("appkit/ziggrid/demux",
  [],
  function() {
    "use strict";

    // TODO: better place for this? global var exports ftw

    var demux = {
      lastId: 0
    };



    return demux;
  });
define("appkit/ziggrid/generator",
  ["appkit/flags"],
  function(flags) {
    "use strict";

    function Generator(url, callback) {
      var open = {
        url: url + 'generator',
        transport: 'websocket',
        fallbackTransport: 'long-polling',

        onOpen: function(response) {
          if (flags.LOG_WEBSOCKETS) {
            console.log('opened generator connection with response', response);
          }
        },

        // and then handle each incoming message
        onMessage: function(msg) {
          if (msg.status === 200) {
            if (flags.LOG_WEBSOCKETS) {
              console.log(msg.responseBody);
            }
            var body = JSON.parse(msg.responseBody);
          } else {
            console.log('Generator HTTP Error:', msg.status);
          }
        }
      };

      var conn = this.conn = jQuery.atmosphere.subscribe(open);
    }

    Generator.prototype = {

      hasSetDelay: false,

      send: function(msg) {
        console.log('Sending generator message', msg);
        this.conn.push(msg);
      },

      start: function() {
        if (!this.hasSetDelay) {
          // Don't overload the generator; give it a moderate delay the first time.
          this.setDelay(20);
          this.hasSetDelay = true;
        }

        this.send(JSON.stringify({'action':'start'}));
      },

      stop: function() {
        this.send(JSON.stringify({'action':'stop'}));
      },

      setDelay: function(ms) {
        this.send(JSON.stringify({'action':'delay','size':ms}));
      }
    };

    Generator.create = function(url, callback) {
      return new Generator(url, callback);
    };



    return Generator;
  });
define("appkit/ziggrid/observer",
  ["appkit/ziggrid/demux","appkit/flags"],
  function(demux, flags) {
    "use strict";

    function Observer(url, callback) {

      url = url + 'updates';

      if (flags.LOG_WEBSOCKETS) {
        console.log('Observer connecting at ' + url);
      }

      var conn = jQuery.atmosphere.subscribe({
        url: url,

        transport: 'websocket',
        fallbackTransport: 'long-polling',

        onOpen: function(response) {
          callback(conn);
        },

        onMessage: function(msg) {
          if (msg.status === 200) {
            // TODO: why are we still getting deliveryFor messages in connectionManager?
            if (flags.LOG_WEBSOCKETS) {
              console.log('Received message ' + msg.responseBody);
            }
            var body = JSON.parse(msg.responseBody);
            if (body['deliveryFor']) {
              var h = demux[body['deliveryFor']];
              if (h && h.update)
    //            h.update(body['table']);
                h.update(body['payload']);
            } else {
              console.error('unknown message type');
            }
          } else {
            console.error('HTTP error');
          }
        }
      });
    }

    Observer.create = function(url, callback) {
      return new Observer(url, callback);
    };


    return Observer;
  });
define("appkit/ziggrid/watcher",
  ["appkit/ziggrid/demux"],
  function(demux) {
    "use strict";

    var container;

    function Loader(type, entryType, id) {
      var store = container.lookup('store:main');

      this.update = type === entryType ? updateIndividualThing : updateTabularData;

      function updateTabularData(body) {
        var rows = [];

        for (var i = 0; i < body.length; i++) {
          var item = body[i];

          var attrs = {};
          attrs[Ember.keys(entryType.model)[0]] = item[0];

          store.load(entryType, item[1], attrs);
          rows.push(item[1]);
        }

        store.load(type, id, {
          table: rows
        });
      }

      function updateIndividualThing(body) {
        body.handle_id = id;
        store.load(type, id, body);
      }
    }

    function Watcher(_namespace) {
      this.namespace = _namespace;
      container = this.container = _namespace.__container__;
    }

    var gameDates = [];

    Watcher.prototype = {
      watchGameDate: function() {
        var handle = ++demux.lastId;

        demux[handle] = {
          update: function(a) {
            gameDates.pushObject(a);
          }
        };

        var query = {
          watch: 'GameDate',
          unique: handle
        };

        var stringified = JSON.stringify(query);

        var connectionManager = container.lookup('connection_manager:main');
        connectionManager.send(stringified);

        //this.sendFakeGameDates();

        return gameDates;
      },

      keepSendingGameDates: false,
      sendFakeGameDates: function() {

        if (this.keepSendingGameDates) {
          gameDates.pushObject({
            day: gameDates.length
          });
        }

        Ember.run.later(this, 'sendFakeGameDates', 400);
      },

      watch: function(typeName, entryTypeName, opts, updateHandler) {
        var type = this.namespace[typeName]; // ED limitation
        var handle = ++demux.lastId;
        var store = container.lookup('store:main');

        store.load(type, handle, {});

        var model = store.find(type, handle);
        var hash = $.extend({
          watch: typeName,
          unique: handle
        }, opts);

        var entryType = this.namespace[entryTypeName];

        demux[handle] = updateHandler ? { update: updateHandler } :
                        new Loader(type, entryType, model.get('id'), opts);

        var stringified = JSON.stringify(hash);

        // TODO: Change this to forward to ZiggridObserver.

        // Send the JSON message to the server to begin observing.
        var connectionManager = container.lookup('connection_manager:main');
        connectionManager.send(stringified);

        return model;
      },

      unwatch: function(handle) {
        var connectionManager = container.lookup('connection_manager:main');
        connectionManager.send(JSON.stringify({ unwatch: handle }));
      }
    };


    return Watcher;
  });
//@ sourceMappingURL=app.js.map