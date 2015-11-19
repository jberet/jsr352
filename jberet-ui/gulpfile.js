'use strict';

var distDir = 'dist/';
var distCssDir = distDir + 'css';

var gulp = require('gulp');
var plugins = require('gulp-load-plugins')();
var del = require('del');
var buffer = require('vinyl-buffer');
var source = require('vinyl-source-stream');
var browserify = require('browserify');
var browserSync = require('browser-sync');
var watchify = require('watchify');
var assign = require('lodash.assign');
var preprocessify = require('preprocessify');
var argv = require('yargs').argv;
var runSequence = require('run-sequence');
var config = require('./config.json');

var files = {
    myjs: ['app/**/*.js', '!app/bower_components/**', '!app/**/*test.js', '!app/**/e2e-tests/**'],

    mycss: ['app/app.css'],

    html: ['app/**/*.html', '!app/e2e-tests/**', '!app/bower_components/**'],

    css: [
        'app/bower_components/html5-boilerplate/dist/css/normalize.css',
        'app/bower_components/html5-boilerplate/dist/css/main.css',
        'app/bower_components/bootstrap/dist/css/bootstrap.css',
        'app/bower_components/angular-ui-grid/ui-grid.css',
        'app/bower_components/dangle/css/dangle.css',
        'app/ui-grid-sky-theme.css',
        'app/app.css'
    ],

    img: 'app/img/**',

    uiGridFont: [
        'app/bower_components/angular-ui-grid/ui-grid.eot',
        'app/bower_components/angular-ui-grid/ui-grid.svg',
        'app/bower_components/angular-ui-grid/ui-grid.ttf',
        'app/bower_components/angular-ui-grid/ui-grid.woff'
    ],

    bootstrapFont: ['app/bower_components/bootstrap/dist/fonts/*'],

    dist: ['dist/**/*.html', 'dist/css/**', 'dist/fonts/**', 'dist/img/**', 'dist/*.js']
};

/**
 * JBeret REST API URL is obtained in the following order:
 * 1, from gulp command line args, e.g., gulp --restUrl "http://example.com/myapp/api";
 * 2, from ./config.json restUrl property;
 * 3, from environment variable JBERET_REST_URL;
 * 4, default value '/jberet-api'
 */
function getRestUrl() {
    return argv.restUrl || config.restUrl || process.env.JBERET_REST_URL || '/api';
}

/**
 * debug should be false for production build.  Development build may choose to turn it on.
 * When debug is set to true, Angular $log debug is enabled, and javascript and css are not minified.
 * When debug is false, Angular $log is disabled, images are optimized, and javascript and css are uglified and minified.
 *
 * debug can be configured in one of the following ways, in order of precedence:
 * 1, from gulp command line args, e.g., gulp --debug
 * 2, from ./config.json debug property
 * 3, defaults to false.
 */
function isDebug() {
    return argv.debug || config.debug || false;
}

var customOpts = {
    entries: ['app/app.js']
    //debug when creating bundles to have Browserify automatically include Source Maps for easy debugging.
    //debug: true
};
var opts = assign({}, watchify.args, customOpts);
var b = watchify(browserify(opts));

gulp.task('js', bundle);

b.on('update', bundle);

b.on('log', plugins.util.log);

function bundle() {
    //This will replace /* @echo __REST_URL__ */ with real value
    //and replace __DEBUG__ with real value
    b.transform(preprocessify({
        '__REST_URL__': getRestUrl(),
        '__DEBUG__': isDebug()
    }));

    return b.bundle()
        .on('error', plugins.util.log.bind(plugins.util, 'Browserify Error'))
        .pipe(source('bundle.js'))

        //minify with source map file
        .pipe(buffer())
        //.pipe(plugins.sourcemaps.init({loadMaps: true}))
        .pipe(plugins.if(!isDebug(), plugins.uglify()))
        // Add transformation tasks to the pipeline here.
        //.pipe(plugins.sourcemaps.write('./'))

        .pipe(gulp.dest(distDir));
}

gulp.task('img', function () {
    return gulp.src(files.img)
        .pipe(plugins.if(!isDebug(), plugins.imagemin()))
        .pipe(gulp.dest(distDir + '/img'));
});


gulp.task('lint', ['jshint', 'csslint']);

gulp.task('jshint', function () {
    return gulp.src(files.myjs)
        .pipe(plugins.jshint())
        .pipe(plugins.jshint.reporter('default'))
});

gulp.task('csslint', function () {
    return gulp.src(files.mycss)
        .pipe(plugins.csslint())
        .pipe(plugins.csslint.reporter())
});


gulp.task('css', function () {
    return gulp.src(files.css)
        //.pipe(plugins.sourcemaps.init({loadMaps: true}))
        .pipe(plugins.concat('bundle.css'))
        .pipe(plugins.if(!isDebug(), plugins.minifyCss()))
        //.pipe(plugins.sourcemaps.write('./'))
        .pipe(gulp.dest(distCssDir));
});

gulp.task('html', function () {
    return gulp.src(files.html, {base: './app'})
        .pipe(gulp.dest(distDir))
});


gulp.task('font', ['bootstrap-font', 'ui-grid-font']);

gulp.task('bootstrap-font', function () {
    //bootstrap css (bundled in css/bundle.css) references font files in a sibling dir (../fonts)
    return gulp.src(files.bootstrapFont)
        .pipe(gulp.dest(distDir + 'fonts'));
});

gulp.task('ui-grid-font', function () {
    //angular-ui-grid css references font files in the same directory
    return gulp.src(files.uiGridFont)
        .pipe(gulp.dest(distCssDir));
});


/**
 * To achieve live update and reload:
 * 1, watchify watches for any js file updates and run browserify when needed;
 * 2, gulp watch task watches for any non-js file updates and run relevant gulp tasks to sync up contents to dist dir;
 * 3, browser-sync watches for any updates in dist dir, and push the new content to browser, including performing
 * css injection.
 */
gulp.task('serve', function(done) {
   runSequence('watch', 'serve-only', done);
});

/**
 * Just start browser-sync server, without running the 'build' task. This task is typically used when you know there is
 * no new changes to be built. Any javascript file changes will still be automatically sync'ed to browser, but other
 * files (html, image, css) will not.
 */
gulp.task('serve-only', function () {
    return browserSync.init(files.dist, {
        server: {
            baseDir: distDir
        }
    });
});

gulp.task('watch', ['build'], function () {
    gulp.watch(files.html, ['html']);
    gulp.watch(files.img, ['img']);
    gulp.watch(files.mycss, ['csslint', 'css']);
    gulp.watch(files.bootstrapFont, ['bootstrap-font']);
    gulp.watch(files.uiGridFont, ['ui-grid-font']);
});

gulp.task('clean', function () {
    return del([distDir + '/**']);
});

gulp.task('build', ['lint', 'img', 'js', 'css', 'html', 'font'], function () {

});

gulp.task('default', ['serve']);
