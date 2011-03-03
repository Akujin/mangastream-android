<?php
//phpinfo();
//die();
include('db.php');
db_connect();

$HTML['report'] = <<< EOF
<li>
    <div>
        <span>{TIMESTAMP} | Phone Model: {MODEL} | Android Version: {ANDROID} | App Version: {VERSION} | MD5: {MD5} | <span id="open_{ID}" style="cursor:pointer;" onclick="javascript:$('trace_{ID}').show();$('open_{ID}').hide();$('close_{ID}').show();">Open</span><span id="close_{ID}" style="cursor:pointer;display:none;" onclick="javascript:$('trace_{ID}').hide();$('open_{ID}').show();$('close_{ID}').hide();">Close</span></span>
        <span id="trace_{ID}" style="display: none;"><pre>{TRACE}</pre></span>
    </div>
    <br/>
</li>

EOF;

$HTML['top10'] = <<< EOF
<li>
    <div>
        <span>{COUNT} | <a href="http://{$_SERVER["HTTP_HOST"]}{$_SERVER["PHP_SELF"]}?md5={MD5}">{MD5}</a></span>
    </div>
</li>
EOF;
?>
<html><head>
<title>MangaStream - Bug Reports</title>
<script src="http://www.prototypejs.org/javascripts/prototype.js" type="text/javascript"></script>
</head><body>


Top 10 Crash Reports
<ul>
<li>Occurences | Hash / Link</li>
<?php
$reports = db_all("SELECT count(*) as `count`,`stacktrace`,`md5` FROM `mangastream_bugreports` GROUP BY `md5` ORDER BY `count` DESC LIMIT 10");
foreach($reports as $report) {
    echo str_replace(array('{COUNT}','{MD5}'),array($report['count'],$report['md5']),$HTML['top10']);
}
?>
</ul>
<ul>

<?php

if(isset($_GET['md5']) && strlen($_GET['md5']) == 32) {
    $md5 = mysql_real_escape_string($_GET['md5']);
   $q = "SELECT * FROM `mangastream_bugreports` WHERE `md5`='{$md5}' ORDER BY `timestamp` DESC";
   ?>
   <li><b>Showing bug reports matching the MD5 Hash string: <?php echo $md5; ?>.</b> | <a href="http://<?php echo $_SERVER["HTTP_HOST"].$_SERVER["PHP_SELF"]; ?>">Go back</a></li>
   <?php
}
else {
    $q = "SELECT * FROM `mangastream_bugreports` ORDER BY `timestamp` DESC";
}
db_q($q,"reports");

if (db_nr("reports")) {
	while($row = db_r("reports")) {
		echo str_replace(array('{ID}','{TIMESTAMP}','{TRACE}','{VERSION}','{MD5}','{MODEL}','{ANDROID}'),array($row['id'],$row['timestamp'],$row['stacktrace'],$row['version'],$row['md5'],$row['phone_model'],$row['android_version']),$HTML['report']);
	}
}

?>
</ul>
</body>
</html>
