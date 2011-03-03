<?php
include('db.php');

function doReport($input) {
    if(empty($input['stacktrace'])) { exit; }
    
    $md5 = md5($input['stacktrace']);
    
    $input['stacktrace'] = db_escape($input['stacktrace'],5000);
    $input['version'] = db_escape($input['version']);
    
    db_connect();
    db_n("INSERT INTO `mangastream_bugreports` (`stacktrace`,`version`,`md5`) VALUES ('{$input['stacktrace']}','{$input['version']}','{$md5}')");
    
}

echo doReport($_POST);

?>
