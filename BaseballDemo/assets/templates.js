Ember.TEMPLATES["application"] = Ember.Handlebars.template(function anonymous(Handlebars,depth0,helpers,partials,data) {
this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Ember.Handlebars.helpers); data = data || {};
  var buffer = '', stack1, helper, options, escapeExpression=this.escapeExpression, helperMissing=helpers.helperMissing, self=this;

function program1(depth0,data) {
  
  var buffer = '';
  data.buffer.push("\n  <div class='nav-button right' ");
  data.buffer.push(escapeExpression(helpers.action.call(depth0, "togglePage", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["STRING"],data:data})));
  data.buffer.push(">&#9658;</div>\n");
  return buffer;
  }

function program3(depth0,data) {
  
  var buffer = '';
  data.buffer.push("\n  <div class='nav-button left' ");
  data.buffer.push(escapeExpression(helpers.action.call(depth0, "togglePage", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["STRING"],data:data})));
  data.buffer.push(">&#9664;</div>\n");
  return buffer;
  }

  data.buffer.push("<div class='container'>\n  <div id='logo-container'>\n    <h1>\n      <span class='ziggrid-text'>Ziggrid</span>\n      <span class='bean-counter-text'>- Beane Counter</span>\n    </h1>\n\n    ");
  data.buffer.push(escapeExpression(helpers.view.call(depth0, "Ember.Select", {hash:{
    'id': ("year-select"),
    'contentBinding': ("years"),
    'valueBinding': ("season")
  },hashTypes:{'id': "STRING",'contentBinding': "STRING",'valueBinding': "STRING"},hashContexts:{'id': depth0,'contentBinding': depth0,'valueBinding': depth0},contexts:[depth0],types:["ID"],data:data})));
  data.buffer.push("\n  </div>\n\n  <div id='page-container' ");
  data.buffer.push(escapeExpression(helpers['bind-attr'].call(depth0, {hash:{
    'style': ("pageContainerStyle")
  },hashTypes:{'style': "STRING"},hashContexts:{'style': depth0},contexts:[],types:[],data:data})));
  data.buffer.push(">\n    ");
  data.buffer.push(escapeExpression((helper = helpers.partial || (depth0 && depth0.partial),options={hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["STRING"],data:data},helper ? helper.call(depth0, "grid", options) : helperMissing.call(depth0, "partial", "grid", options))));
  data.buffer.push("\n    ");
  data.buffer.push(escapeExpression((helper = helpers.render || (depth0 && depth0.render),options={hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["STRING"],data:data},helper ? helper.call(depth0, "quadrant", options) : helperMissing.call(depth0, "render", "quadrant", options))));
  data.buffer.push("\n  </div>\n</div>\n\n");
  stack1 = helpers.unless.call(depth0, "currentPage", {hash:{},hashTypes:{},hashContexts:{},inverse:self.noop,fn:self.program(1, program1, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n\n");
  stack1 = helpers['if'].call(depth0, "currentPage", {hash:{},hashTypes:{},hashContexts:{},inverse:self.noop,fn:self.program(3, program3, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n");
  return buffer;
  
});

Ember.TEMPLATES["components/bean-player-profile"] = Ember.Handlebars.template(function anonymous(Handlebars,depth0,helpers,partials,data) {
this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Ember.Handlebars.helpers); data = data || {};
  var buffer = '', stack1, helperMissing=helpers.helperMissing, escapeExpression=this.escapeExpression, self=this;

function program1(depth0,data) {
  
  var buffer = '', stack1;
  data.buffer.push("\n  ");
  stack1 = helpers['with'].call(depth0, "profile", {hash:{},hashTypes:{},hashContexts:{},inverse:self.noop,fn:self.program(2, program2, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n");
  return buffer;
  }
function program2(depth0,data) {
  
  var buffer = '', stack1, helper, options;
  data.buffer.push("\n    <table class='metrics'>\n      <tr>\n        <td>At Bats</td>\n        <td class='value'>");
  stack1 = helpers._triageMustache.call(depth0, "atbats", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("</td>\n      </tr>\n      <tr>\n        <td>Hits</td>\n        <td class='value'>");
  stack1 = helpers._triageMustache.call(depth0, "hits", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("</td>\n      </tr>\n      <tr>\n        <td>Walks</td>\n        <td class='value'>");
  stack1 = helpers._triageMustache.call(depth0, "walks", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("</td>\n      </tr>\n      <tr>\n        <td>Homeruns</td>\n        <td class='value'>");
  stack1 = helpers._triageMustache.call(depth0, "hrs", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("</td>\n      </tr>\n      <tr>\n        <td>Goodness</td>\n        <td class='value'>");
  data.buffer.push(escapeExpression((helper = helpers['quadrant-value'] || (depth0 && depth0['quadrant-value']),options={hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data},helper ? helper.call(depth0, "clutchness", options) : helperMissing.call(depth0, "quadrant-value", "clutchness", options))));
  data.buffer.push("</td>\n      </tr>\n      <tr>\n        <td>Hotness</td>\n        <td class='value'>");
  data.buffer.push(escapeExpression((helper = helpers['quadrant-value'] || (depth0 && depth0['quadrant-value']),options={hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data},helper ? helper.call(depth0, "hotness", options) : helperMissing.call(depth0, "quadrant-value", "hotness", options))));
  data.buffer.push("</td>\n      </tr>\n    </table>\n  ");
  return buffer;
  }

function program4(depth0,data) {
  
  
  data.buffer.push("\n  <p class='no-data-yet'>No profile data received yet...</p>\n");
  }

  data.buffer.push(escapeExpression(helpers.view.call(depth0, "Ember.Select", {hash:{
    'id': ("player-profile-select"),
    'contentBinding': ("players"),
    'optionValueBinding': ("content"),
    'optionLabelPath': ("content.PlayerName"),
    'valueBinding': ("player")
  },hashTypes:{'id': "STRING",'contentBinding': "STRING",'optionValueBinding': "STRING",'optionLabelPath': "STRING",'valueBinding': "STRING"},hashContexts:{'id': depth0,'contentBinding': depth0,'optionValueBinding': depth0,'optionLabelPath': depth0,'valueBinding': depth0},contexts:[depth0],types:["ID"],data:data})));
  data.buffer.push("\n");
  stack1 = helpers['if'].call(depth0, "profile", {hash:{},hashTypes:{},hashContexts:{},inverse:self.program(4, program4, data),fn:self.program(1, program1, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n\n<div id='profile-image-container'>\n  <img ");
  data.buffer.push(escapeExpression(helpers['bind-attr'].call(depth0, {hash:{
    'src': ("imageUrl")
  },hashTypes:{'src': "STRING"},hashContexts:{'src': depth0},contexts:[],types:[],data:data})));
  data.buffer.push(" />\n</div>\n\n");
  return buffer;
  
});

Ember.TEMPLATES["components/bean-player"] = Ember.Handlebars.template(function anonymous(Handlebars,depth0,helpers,partials,data) {
this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Ember.Handlebars.helpers); data = data || {};
  var buffer = '', stack1, escapeExpression=this.escapeExpression, helperMissing=helpers.helperMissing, self=this;

function program1(depth0,data) {
  
  var buffer = '', stack1, helper, options;
  data.buffer.push("\n    <button ");
  data.buffer.push(escapeExpression(helpers['bind-attr'].call(depth0, {hash:{
    'class': (":play-button isPlaying:inactive")
  },hashTypes:{'class': "STRING"},hashContexts:{'class': depth0},contexts:[],types:[],data:data})));
  data.buffer.push(" ");
  data.buffer.push(escapeExpression(helpers.action.call(depth0, "play", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["STRING"],data:data})));
  data.buffer.push(">&#9658;</button>\n    <button ");
  data.buffer.push(escapeExpression(helpers['bind-attr'].call(depth0, {hash:{
    'class': (":pause-button isPlaying::inactive")
  },hashTypes:{'class': "STRING"},hashContexts:{'class': depth0},contexts:[],types:[],data:data})));
  data.buffer.push(" ");
  data.buffer.push(escapeExpression(helpers.action.call(depth0, "pause", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["STRING"],data:data})));
  data.buffer.push(">&#10074;&#10074;</button>\n\n    <div class='player-progress-bar-container'>\n\n      ");
  stack1 = helpers['if'].call(depth0, "progressText", {hash:{},hashTypes:{},hashContexts:{},inverse:self.noop,fn:self.program(2, program2, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n\n      <div class='player-progress-bar' ");
  data.buffer.push(escapeExpression(helpers['bind-attr'].call(depth0, {hash:{
    'style': ("progressBarStyle")
  },hashTypes:{'style': "STRING"},hashContexts:{'style': depth0},contexts:[],types:[],data:data})));
  data.buffer.push("></div>\n\n      ");
  stack1 = helpers['if'].call(depth0, "showNub", {hash:{},hashTypes:{},hashContexts:{},inverse:self.noop,fn:self.program(4, program4, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n    </div>\n    <div class='label'>\n      <span class='count'>");
  data.buffer.push(escapeExpression((helper = helpers.precision || (depth0 && depth0.precision),options={hash:{},hashTypes:{},hashContexts:{},contexts:[depth0,depth0],types:["ID","INTEGER"],data:data},helper ? helper.call(depth0, "progressPercentage", 100, options) : helperMissing.call(depth0, "precision", "progressPercentage", 100, options))));
  data.buffer.push("</span>&nbsp;%\n    </div>\n  ");
  return buffer;
  }
function program2(depth0,data) {
  
  var buffer = '', stack1;
  data.buffer.push("\n        <div class='player-progress-text-display' ");
  data.buffer.push(escapeExpression(helpers['bind-attr'].call(depth0, {hash:{
    'style': ("progressTextStyle")
  },hashTypes:{'style': "STRING"},hashContexts:{'style': depth0},contexts:[],types:[],data:data})));
  data.buffer.push(">");
  stack1 = helpers._triageMustache.call(depth0, "progressText", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("</div>\n      ");
  return buffer;
  }

function program4(depth0,data) {
  
  var buffer = '', helper, options;
  data.buffer.push("\n        ");
  data.buffer.push(escapeExpression((helper = helpers.input || (depth0 && depth0.input),options={hash:{
    'value': ("nubProgress"),
    'step': ("1"),
    'min': ("0"),
    'max': ("926"),
    'type': ("range"),
    'class': ("player-range-input")
  },hashTypes:{'value': "ID",'step': "STRING",'min': "STRING",'max': "STRING",'type': "STRING",'class': "STRING"},hashContexts:{'value': depth0,'step': depth0,'min': depth0,'max': depth0,'type': depth0,'class': depth0},contexts:[],types:[],data:data},helper ? helper.call(depth0, options) : helperMissing.call(depth0, "input", options))));
  data.buffer.push("\n      ");
  return buffer;
  }

function program6(depth0,data) {
  
  
  data.buffer.push("\n    <p>Waiting for generator...</p>\n  ");
  }

  data.buffer.push("<div class='player'>\n  ");
  stack1 = helpers['if'].call(depth0, "generators", {hash:{},hashTypes:{},hashContexts:{},inverse:self.program(6, program6, data),fn:self.program(1, program1, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n</div>\n");
  return buffer;
  
});

Ember.TEMPLATES["components/bean-quadrant"] = Ember.Handlebars.template(function anonymous(Handlebars,depth0,helpers,partials,data) {
this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Ember.Handlebars.helpers); data = data || {};
  var buffer = '', helper, options, helperMissing=helpers.helperMissing, escapeExpression=this.escapeExpression;


  data.buffer.push("<div class='quadrant-container'>\n  ");
  data.buffer.push(escapeExpression((helper = helpers.render || (depth0 && depth0.render),options={hash:{},hashTypes:{},hashContexts:{},contexts:[depth0,depth0],types:["ID","ID"],data:data},helper ? helper.call(depth0, "quadrant_player", "selectedPlayer", options) : helperMissing.call(depth0, "render", "quadrant_player", "selectedPlayer", options))));
  data.buffer.push("\n  \n  <div class='quadrant-graph'></div>\n\n  <div class='labels y-labels'>\n    <label class='min'>&#9664; Slump</label>\n    <label class='max'>Streak &#9658;</label>\n    <label class='label'>Hotness</label>\n  </div>\n</div>\n\n<div class='labels x-labels'>\n  <label class='min'>&#9664; Chump</label>\n  <label class='max'>Champ &#9658;</label>\n  <label class='label'>Goodness</label>\n</div>\n");
  return buffer;
  
});

Ember.TEMPLATES["components/bean-standings"] = Ember.Handlebars.template(function anonymous(Handlebars,depth0,helpers,partials,data) {
this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Ember.Handlebars.helpers); data = data || {};
  var buffer = '', stack1, helperMissing=helpers.helperMissing, escapeExpression=this.escapeExpression, self=this;

function program1(depth0,data) {
  
  var buffer = '', stack1;
  data.buffer.push("\n        <th>");
  stack1 = helpers._triageMustache.call(depth0, "", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("</th>\n      ");
  return buffer;
  }

function program3(depth0,data) {
  
  var buffer = '', helper, options;
  data.buffer.push("\n      ");
  data.buffer.push(escapeExpression((helper = helpers['bean-team-standing'] || (depth0 && depth0['bean-team-standing']),options={hash:{
    'team': ("")
  },hashTypes:{'team': "ID"},hashContexts:{'team': depth0},contexts:[],types:[],data:data},helper ? helper.call(depth0, options) : helperMissing.call(depth0, "bean-team-standing", options))));
  data.buffer.push("\n    ");
  return buffer;
  }

  data.buffer.push("<table class='metrics'>\n  <thead>\n    <tr>\n      ");
  stack1 = helpers.each.call(depth0, "headers", {hash:{},hashTypes:{},hashContexts:{},inverse:self.noop,fn:self.program(1, program1, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n    </tr>\n  </thead>\n  <tbody>\n    ");
  stack1 = helpers.each.call(depth0, "region.teams", {hash:{},hashTypes:{},hashContexts:{},inverse:self.noop,fn:self.program(3, program3, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n  </tbody>\n</table>\n");
  return buffer;
  
});

Ember.TEMPLATES["components/bean-table"] = Ember.Handlebars.template(function anonymous(Handlebars,depth0,helpers,partials,data) {
this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Ember.Handlebars.helpers); data = data || {};
  var buffer = '', stack1, helperMissing=helpers.helperMissing, escapeExpression=this.escapeExpression, self=this;

function program1(depth0,data) {
  
  var buffer = '', stack1;
  data.buffer.push("\n        <th>");
  stack1 = helpers._triageMustache.call(depth0, "", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("</th>\n      ");
  return buffer;
  }

function program3(depth0,data) {
  
  var buffer = '', stack1, helper, options;
  data.buffer.push("\n      <tr>\n        <td>");
  data.buffer.push(escapeExpression((helper = helpers['name-from-code'] || (depth0 && depth0['name-from-code']),options={hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data},helper ? helper.call(depth0, "id", options) : helperMissing.call(depth0, "name-from-code", "id", options))));
  data.buffer.push("</td>\n        <td class='value'>\n          ");
  stack1 = helpers['if'].call(depth0, "hrs", {hash:{},hashTypes:{},hashContexts:{},inverse:self.program(6, program6, data),fn:self.program(4, program4, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n        </td>\n      </tr>\n    ");
  return buffer;
  }
function program4(depth0,data) {
  
  var buffer = '', stack1;
  data.buffer.push("\n            ");
  stack1 = helpers._triageMustache.call(depth0, "hrs", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n          ");
  return buffer;
  }

function program6(depth0,data) {
  
  var buffer = '', stack1;
  data.buffer.push("\n            ");
  stack1 = helpers['if'].call(depth0, "average", {hash:{},hashTypes:{},hashContexts:{},inverse:self.program(9, program9, data),fn:self.program(7, program7, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n          ");
  return buffer;
  }
function program7(depth0,data) {
  
  var buffer = '', helper, options;
  data.buffer.push("\n              ");
  data.buffer.push(escapeExpression((helper = helpers.round || (depth0 && depth0.round),options={hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data},helper ? helper.call(depth0, "average", options) : helperMissing.call(depth0, "round", "average", options))));
  data.buffer.push("\n            ");
  return buffer;
  }

function program9(depth0,data) {
  
  var buffer = '', stack1;
  data.buffer.push("\n              ");
  stack1 = helpers['if'].call(depth0, "production", {hash:{},hashTypes:{},hashContexts:{},inverse:self.noop,fn:self.program(10, program10, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n            ");
  return buffer;
  }
function program10(depth0,data) {
  
  var buffer = '', helper, options;
  data.buffer.push("\n                ");
  data.buffer.push(escapeExpression((helper = helpers.round || (depth0 && depth0.round),options={hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data},helper ? helper.call(depth0, "production", options) : helperMissing.call(depth0, "round", "production", options))));
  data.buffer.push("\n              ");
  return buffer;
  }

  data.buffer.push("<table class='metrics'>\n  <thead>\n    <tr>\n      ");
  stack1 = helpers.each.call(depth0, "headers", {hash:{},hashTypes:{},hashContexts:{},inverse:self.noop,fn:self.program(1, program1, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n    </tr>\n  </thead>\n  <tbody>\n    ");
  stack1 = helpers.each.call(depth0, "entries", {hash:{},hashTypes:{},hashContexts:{},inverse:self.noop,fn:self.program(3, program3, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n  </tbody>\n</table>\n");
  return buffer;
  
});

Ember.TEMPLATES["components/bean-team-standing"] = Ember.Handlebars.template(function anonymous(Handlebars,depth0,helpers,partials,data) {
this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Ember.Handlebars.helpers); data = data || {};
  var buffer = '', stack1, self=this;

function program1(depth0,data) {
  
  var buffer = '', stack1;
  data.buffer.push("\n      ");
  stack1 = helpers._triageMustache.call(depth0, "wins", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n    ");
  return buffer;
  }

function program3(depth0,data) {
  
  
  data.buffer.push("\n      --\n    ");
  }

function program5(depth0,data) {
  
  var buffer = '', stack1;
  data.buffer.push("\n      ");
  stack1 = helpers._triageMustache.call(depth0, "losses", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n    ");
  return buffer;
  }

  data.buffer.push("<tr>\n  <td>");
  stack1 = helpers._triageMustache.call(depth0, "team.code", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("</td>\n  <td class='value'>\n    ");
  stack1 = helpers['if'].call(depth0, "wins", {hash:{},hashTypes:{},hashContexts:{},inverse:self.program(3, program3, data),fn:self.program(1, program1, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n  </td>\n  <td class='value'>\n    ");
  stack1 = helpers['if'].call(depth0, "losses", {hash:{},hashTypes:{},hashContexts:{},inverse:self.program(3, program3, data),fn:self.program(5, program5, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n  </td>\n</tr>\n");
  return buffer;
  
});

Ember.TEMPLATES["filter"] = Ember.Handlebars.template(function anonymous(Handlebars,depth0,helpers,partials,data) {
this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Ember.Handlebars.helpers); data = data || {};
  var buffer = '', stack1, escapeExpression=this.escapeExpression, self=this;

function program1(depth0,data) {
  
  var buffer = '', stack1;
  data.buffer.push("\n    <li ");
  data.buffer.push(escapeExpression(helpers['bind-attr'].call(depth0, {hash:{
    'class': ("isSelected")
  },hashTypes:{'class': "STRING"},hashContexts:{'class': depth0},contexts:[],types:[],data:data})));
  data.buffer.push(" ");
  data.buffer.push(escapeExpression(helpers.action.call(depth0, "selectFilter", "content", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0,depth0],types:["STRING","ID"],data:data})));
  data.buffer.push(">");
  stack1 = helpers._triageMustache.call(depth0, "content", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("</li>\n  ");
  return buffer;
  }

  data.buffer.push("<h2>Filtered By</h2>\n<h3>All Star Class</h3>\n\n<ul>\n  ");
  stack1 = helpers.each.call(depth0, "years", {hash:{
    'itemController': ("filter_item")
  },hashTypes:{'itemController': "STRING"},hashContexts:{'itemController': depth0},inverse:self.noop,fn:self.program(1, program1, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n</ul>\n\n");
  return buffer;
  
});

Ember.TEMPLATES["grid"] = Ember.Handlebars.template(function anonymous(Handlebars,depth0,helpers,partials,data) {
this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Ember.Handlebars.helpers); data = data || {};
  var buffer = '', stack1, helper, options, escapeExpression=this.escapeExpression, helperMissing=helpers.helperMissing;


  data.buffer.push("<div ");
  data.buffer.push(escapeExpression(helpers['bind-attr'].call(depth0, {hash:{
    'class': (":page currentPage::showing")
  },hashTypes:{'class': "STRING"},hashContexts:{'class': depth0},contexts:[],types:[],data:data})));
  data.buffer.push(" id='grid-page'>\n  <div class='grid'>\n\n    <div id='standings-container'>\n\n      <h1 class='stats-header'>Standings</h1>\n\n      <div class='box cf'>\n        ");
  data.buffer.push(escapeExpression((helper = helpers.render || (depth0 && depth0.render),options={hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data},helper ? helper.call(depth0, "standings", options) : helperMissing.call(depth0, "render", "standings", options))));
  data.buffer.push("\n      </div>\n    </div>\n\n    <div class='grid-33 statistics-table'>\n      <h1 class='stats-header'>Statistics</h1>\n      <div class='box'>\n        <h1>Statistics</h1>\n        ");
  data.buffer.push(escapeExpression((helper = helpers['bean-leaderboard'] || (depth0 && depth0['bean-leaderboard']),options={hash:{
    'season': ("season")
  },hashTypes:{'season': "ID"},hashContexts:{'season': depth0},contexts:[],types:[],data:data},helper ? helper.call(depth0, options) : helperMissing.call(depth0, "bean-leaderboard", options))));
  data.buffer.push("\n        ");
  data.buffer.push(escapeExpression((helper = helpers['bean-production'] || (depth0 && depth0['bean-production']),options={hash:{
    'season': ("season")
  },hashTypes:{'season': "ID"},hashContexts:{'season': depth0},contexts:[],types:[],data:data},helper ? helper.call(depth0, options) : helperMissing.call(depth0, "bean-production", options))));
  data.buffer.push("\n        ");
  data.buffer.push(escapeExpression((helper = helpers['bean-homeruns'] || (depth0 && depth0['bean-homeruns']),options={hash:{
    'season': ("season")
  },hashTypes:{'season': "ID"},hashContexts:{'season': depth0},contexts:[],types:[],data:data},helper ? helper.call(depth0, options) : helperMissing.call(depth0, "bean-homeruns", options))));
  data.buffer.push("\n      </div>\n    </div>\n\n    <div class='grid-33'>\n      <h1 class='stats-header'>Player Profile</h1>\n      <div class='box'>\n        ");
  stack1 = helpers._triageMustache.call(depth0, "bean-player-profile", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n      </div>\n    </div>\n  </div>\n\n  \n</div>\n\n\n");
  return buffer;
  
});

Ember.TEMPLATES["nav"] = Ember.Handlebars.template(function anonymous(Handlebars,depth0,helpers,partials,data) {
this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Ember.Handlebars.helpers); data = data || {};
  var buffer = '', stack1, escapeExpression=this.escapeExpression, self=this;

function program1(depth0,data) {
  
  var buffer = '', stack1;
  data.buffer.push("\n                  <li><a href='#' ");
  data.buffer.push(escapeExpression(helpers.action.call(depth0, "showSeason", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["STRING"],data:data})));
  data.buffer.push(">");
  stack1 = helpers._triageMustache.call(depth0, "", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("</a></li>\n                ");
  return buffer;
  }

  data.buffer.push("<nav>\n  <div class='navbar navbar-fixed-top'>\n    <div class='navbar-inner' style='height: 60px'>\n      <div class='container-fluid'>\n        <div class='row'>  \n          <div class='span6'>  \n            <div style='color: #ddd; font-size: 39pt; float:left; margin-top: 16px; margin-right: 20px'>Ziggrid</div>\n            <div style='display: inline-block;'>\n              <ul id='tab-nav' class='nav nav-pills'>\n                ");
  stack1 = helpers.each.call(depth0, "seasons", {hash:{},hashTypes:{},hashContexts:{},inverse:self.noop,fn:self.program(1, program1, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n              </ul> \n            </div>  \n          </div>  \n        </div> \n      </div>\n    </div>\n  </div>\n</nav>\n\n");
  return buffer;
  
});

Ember.TEMPLATES["quadrant"] = Ember.Handlebars.template(function anonymous(Handlebars,depth0,helpers,partials,data) {
this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Ember.Handlebars.helpers); data = data || {};
  var buffer = '', helper, options, escapeExpression=this.escapeExpression, helperMissing=helpers.helperMissing;


  data.buffer.push("<div ");
  data.buffer.push(escapeExpression(helpers['bind-attr'].call(depth0, {hash:{
    'class': (":page showing")
  },hashTypes:{'class': "STRING"},hashContexts:{'class': depth0},contexts:[],types:[],data:data})));
  data.buffer.push(" id=\"quadrant-page\">\n  <div class=\"page-content\">\n    <h1 class=\"stats-header\">Magic Quadrant Analysis</h1>\n\n    ");
  data.buffer.push(escapeExpression((helper = helpers.render || (depth0 && depth0.render),options={hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["STRING"],data:data},helper ? helper.call(depth0, "filter", options) : helperMissing.call(depth0, "render", "filter", options))));
  data.buffer.push("\n\n    ");
  data.buffer.push(escapeExpression((helper = helpers['bean-quadrant'] || (depth0 && depth0['bean-quadrant']),options={hash:{
    'season': ("filter"),
    'players': ("players"),
    'watchedPlayers': ("watchedQuadrantPlayers")
  },hashTypes:{'season': "ID",'players': "ID",'watchedPlayers': "ID"},hashContexts:{'season': depth0,'players': depth0,'watchedPlayers': depth0},contexts:[],types:[],data:data},helper ? helper.call(depth0, options) : helperMissing.call(depth0, "bean-quadrant", options))));
  data.buffer.push("\n  </div>\n</div>\n\n");
  data.buffer.push(escapeExpression((helper = helpers['bean-player'] || (depth0 && depth0['bean-player']),options={hash:{
    'showNub': ("showing"),
    'progressText': ("currentDateText"),
    'progress': ("progress"),
    'didBeginPlaying': ("didBeginPlaying"),
    'didEndPlaying': ("didEndPlaying")
  },hashTypes:{'showNub': "ID",'progressText': "ID",'progress': "ID",'didBeginPlaying': "STRING",'didEndPlaying': "STRING"},hashContexts:{'showNub': depth0,'progressText': depth0,'progress': depth0,'didBeginPlaying': depth0,'didEndPlaying': depth0},contexts:[],types:[],data:data},helper ? helper.call(depth0, options) : helperMissing.call(depth0, "bean-player", options))));
  data.buffer.push("\n");
  return buffer;
  
});

Ember.TEMPLATES["quadrant_player"] = Ember.Handlebars.template(function anonymous(Handlebars,depth0,helpers,partials,data) {
this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Ember.Handlebars.helpers); data = data || {};
  var buffer = '', stack1, helperMissing=helpers.helperMissing, escapeExpression=this.escapeExpression, self=this;

function program1(depth0,data) {
  
  var buffer = '', helper, options;
  data.buffer.push("\n    <ul>\n      <li><span>At Bats</span>  ");
  data.buffer.push(escapeExpression((helper = helpers.round || (depth0 && depth0.round),options={hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data},helper ? helper.call(depth0, "profile.atbats", options) : helperMissing.call(depth0, "round", "profile.atbats", options))));
  data.buffer.push("</li>\n      <li><span>Hits</span>     ");
  data.buffer.push(escapeExpression((helper = helpers.round || (depth0 && depth0.round),options={hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data},helper ? helper.call(depth0, "profile.hits", options) : helperMissing.call(depth0, "round", "profile.hits", options))));
  data.buffer.push("</li>\n      <li><span>Walks</span>    ");
  data.buffer.push(escapeExpression((helper = helpers.round || (depth0 && depth0.round),options={hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data},helper ? helper.call(depth0, "profile.walks", options) : helperMissing.call(depth0, "round", "profile.walks", options))));
  data.buffer.push("</li>\n      <li><span>Hrs</span>      ");
  data.buffer.push(escapeExpression((helper = helpers.round || (depth0 && depth0.round),options={hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data},helper ? helper.call(depth0, "profile.hrs", options) : helperMissing.call(depth0, "round", "profile.hrs", options))));
  data.buffer.push("</li>\n      <li><span>Goodness</span> ");
  data.buffer.push(escapeExpression((helper = helpers['quadrant-value'] || (depth0 && depth0['quadrant-value']),options={hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data},helper ? helper.call(depth0, "goodness", options) : helperMissing.call(depth0, "quadrant-value", "goodness", options))));
  data.buffer.push("</li>\n      <li><span>Hotness</span>  ");
  data.buffer.push(escapeExpression((helper = helpers['quadrant-value'] || (depth0 && depth0['quadrant-value']),options={hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data},helper ? helper.call(depth0, "hotness", options) : helperMissing.call(depth0, "quadrant-value", "hotness", options))));
  data.buffer.push("</li>\n    </ul>\n\n    <div class=\"profile-pic-container\">\n      <img class=\"profile-pic\" ");
  data.buffer.push(escapeExpression(helpers['bind-attr'].call(depth0, {hash:{
    'src': ("imageUrl")
  },hashTypes:{'src': "ID"},hashContexts:{'src': depth0},contexts:[],types:[],data:data})));
  data.buffer.push(" />\n    </div>\n  ");
  return buffer;
  }

function program3(depth0,data) {
  
  
  data.buffer.push("\n    <img class=\"loading-spinner\" src=\"/ajax-loader.gif\" alt=\"Loading...\" />\n  ");
  }

function program5(depth0,data) {
  
  var buffer = '', stack1;
  data.buffer.push("\n      ");
  stack1 = helpers._triageMustache.call(depth0, "profile.fullname", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n    ");
  return buffer;
  }

function program7(depth0,data) {
  
  
  data.buffer.push("\n      Loading...\n    ");
  }

  data.buffer.push("<div ");
  data.buffer.push(escapeExpression(helpers['bind-attr'].call(depth0, {hash:{
    'class': (":quadrant-popup content:player-present")
  },hashTypes:{'class': "STRING"},hashContexts:{'class': depth0},contexts:[],types:[],data:data})));
  data.buffer.push(">\n  ");
  stack1 = helpers['if'].call(depth0, "profile", {hash:{},hashTypes:{},hashContexts:{},inverse:self.program(3, program3, data),fn:self.program(1, program1, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n\n  <div class=\"footer\">\n    ");
  stack1 = helpers['if'].call(depth0, "profile", {hash:{},hashTypes:{},hashContexts:{},inverse:self.program(7, program7, data),fn:self.program(5, program5, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n  </div>\n</div>\n");
  return buffer;
  
});

Ember.TEMPLATES["standings"] = Ember.Handlebars.template(function anonymous(Handlebars,depth0,helpers,partials,data) {
this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Ember.Handlebars.helpers); data = data || {};
  var buffer = '', stack1, helperMissing=helpers.helperMissing, escapeExpression=this.escapeExpression, self=this;

function program1(depth0,data) {
  
  var buffer = '', stack1;
  data.buffer.push("\n  <div class='standings-column'>\n    <h1>");
  stack1 = helpers._triageMustache.call(depth0, "league.name", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("</h1>\n\n    ");
  stack1 = helpers.each.call(depth0, "league.regions", {hash:{},hashTypes:{},hashContexts:{},inverse:self.noop,fn:self.program(2, program2, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n  </div>\n");
  return buffer;
  }
function program2(depth0,data) {
  
  var buffer = '', helper, options;
  data.buffer.push("\n      ");
  data.buffer.push(escapeExpression((helper = helpers['bean-standings'] || (depth0 && depth0['bean-standings']),options={hash:{
    'region': ("")
  },hashTypes:{'region': "ID"},hashContexts:{'region': depth0},contexts:[],types:[],data:data},helper ? helper.call(depth0, options) : helperMissing.call(depth0, "bean-standings", options))));
  data.buffer.push("\n    ");
  return buffer;
  }

  stack1 = helpers.each.call(depth0, "league", "in", "leagues", {hash:{},hashTypes:{},hashContexts:{},inverse:self.noop,fn:self.program(1, program1, data),contexts:[depth0,depth0,depth0],types:["ID","ID","ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n");
  return buffer;
  
});

Ember.TEMPLATES["statistics"] = Ember.Handlebars.template(function anonymous(Handlebars,depth0,helpers,partials,data) {
this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Ember.Handlebars.helpers); data = data || {};
  var buffer = '', stack1, self=this;

function program1(depth0,data) {
  
  var buffer = '', stack1;
  data.buffer.push("\n      <tr>\n        <td>2006</td>\n        <td>");
  stack1 = helpers._triageMustache.call(depth0, "id", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("</td>\n        <td class='value'>");
  stack1 = helpers._triageMustache.call(depth0, "f1", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("</td>\n      </tr>\n    ");
  return buffer;
  }

  data.buffer.push("<h1>Batting Leaders</h1>\n<table class='metrics'>\n  <thead>\n    <tr>\n      <th>Season</th>\n      <th>Player</th>\n      <th>Average</th>\n    </tr>\n  </thead>\n  <tbody>\n    ");
  stack1 = helpers.each.call(depth0, {hash:{},hashTypes:{},hashContexts:{},inverse:self.noop,fn:self.program(1, program1, data),contexts:[],types:[],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n  </tbody>\n</table>\n");
  return buffer;
  
});