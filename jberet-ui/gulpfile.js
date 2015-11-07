'use strict';

var distDir = 'dist/';
var distCssDir = distDir + 'css';

var gulp = require('gulp');
var plugins = require('gulp-load-plugins')();
var del = require('del');
var buffer = require('vinyl-buffer');
var source = require('vinyl-source-stream');
var browserify = require('browserify');

gulp.task('image', function () {
    return gulp.src('app/img/**')
        .pipe(plugins.imagemin())
        .pipe(gulp.dest(distDir + '/img'));
});

gulp.task('js', function () {
    var b = browserify({
        entries: 'app/app.js',
        debug: true
    });

    return b.bundle()
        .pipe(source('bundle.js'))

        //minify with source map file
        .pipe(buffer())
        .pipe(plugins.sourcemaps.init({loadMaps: true}))
        .pipe(plugins.uglify())
        .on('error', plugins.util.log)
        .pipe(plugins.sourcemaps.write('./'))

        .pipe(gulp.dest(distDir));
});

gulp.task('lint', function () {
   return  gulp.src(['app/**/*.js', '!app/bower_components/**', '!app/**/*test.js', '!app/**/e2e-tests/**'])
        .pipe(plugins.jshint())
        .pipe(plugins.jshint.reporter('default'))
});

gulp.task('css', function () {
    var cssFiles = [
        'app/bower_components/html5-boilerplate/dist/css/normalize.css',
        'app/bower_components/html5-boilerplate/dist/css/main.css',
        'app/bower_components/bootstrap/dist/css/bootstrap.css',
        'app/bower_components/angular-ui-grid/ui-grid.css',
        'app/bower_components/dangle/css/dangle.css',
        'app/ui-grid-sky-theme.css',
        'app/app.css'
    ];
    return gulp.src(cssFiles)
        .pipe(plugins.sourcemaps.init({loadMaps: true}))
        .pipe(plugins.csslint())
        .pipe(plugins.csslint.reporter())
        .pipe(plugins.concat('bundle.css'))
        .pipe(plugins.minifyCss())
        .pipe(plugins.sourcemaps.write('./'))
        .pipe(gulp.dest(distCssDir));
});

gulp.task('html', function () {
    var htmlFiles = [
        'app/**/*.html',
        '!app/e2e-tests/**',
        '!app/bower_components/**'
    ];
    gulp.src(htmlFiles, {base: './app'})
        .pipe(gulp.dest(distDir))
});

gulp.task('font', function () {
    var uiGridFontFiles = [
        'app/bower_components/angular-ui-grid/ui-grid.eot',
        'app/bower_components/angular-ui-grid/ui-grid.svg',
        'app/bower_components/angular-ui-grid/ui-grid.ttf',
        'app/bower_components/angular-ui-grid/ui-grid.woff'
    ];
    var bootstrapFontFiles = [
      'app/bower_components/bootstrap/dist/fonts/*'
    ];

    //bootstrap css (bundled in css/bundle.css) references font files in a sibling dir (../fonts)
    gulp.src(bootstrapFontFiles)
        .pipe(gulp.dest(distDir + 'fonts'));

    //angular-ui-grid css references font files in the same directory
    return gulp.src(uiGridFontFiles)
        .pipe(gulp.dest(distCssDir));
});

gulp.task('clean', function () {
    return del([distDir + '/**']);
});

gulp.task('build', ['lint', 'image', 'js', 'css', 'html', 'font'], function () {

});

gulp.task('default', ['build']);
