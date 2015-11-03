'use strict';

var dist = 'dist/app';
var gulp = require('gulp');
var plugins = require('gulp-load-plugins')();
var del = require('del');
var source = require('vinyl-source-stream');


gulp.task('image', function () {
    return gulp.src('app/img/**')
        .pipe(plugins.imagemin())
        .pipe(gulp.dest(dist + '/img'));
});

gulp.task('js', function () {
    return gulp.src(['app/**/*.js', '!app/bower_components/**', '!app/**/*test.js', '!app/**/e2e-tests/**'])
        .pipe(plugins.sourcemaps.init())
        .pipe(plugins.jshint())
        .pipe(plugins.jshint.reporter('default'))
        .pipe(plugins.concat('app-min.js'))
        .pipe(plugins.uglify())
        .pipe(plugins.sourcemaps.write())
        .pipe(gulp.dest(dist));
});

gulp.task('css', function () {
    return gulp.src(['app/**/*.css', '!app/bower_components/**'])
        .pipe(plugins.sourcemaps.init())
        .pipe(plugins.csslint())
        .pipe(plugins.csslint.reporter())
        .pipe(plugins.concat('app-min.css'))
        .pipe(plugins.minifyCss())
        .pipe(plugins.sourcemaps.write())
        .pipe(gulp.dest(dist));
});

gulp.task('html', function () {
    var htmlFiles = [
        'app/**/*.html',
        '!app/e2e-tests/**',
        '!app/bower_components/**'
    ];
    gulp.src(htmlFiles, {base: './app'})
        .pipe(gulp.dest(dist))
});

gulp.task('clean', function () {
    return del([dist + '/**']);
});

gulp.task('build', ['image', 'js', 'css', 'html'], function () {

});

gulp.task('default', ['build']);
