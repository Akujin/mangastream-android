<?php
/************************************************************
 *     /   |  / __ \/_  __/  _// ____/  _//   | / /\   / /  *
 *    / /| | / /_/ / / /  / / / /    / / / /| |/ /\ \ / /   *
 *   / ___ |/ _, _/ / /  / / / /____/ / / ___ / /  \ / /    *
 *  /_/  |_/_/ |_| /_/ /___/ \____/___//_/  |/ /    / /     *
 ************************************************************
 * This file is a MySQL wrapper driver which wraps around   *
 * PHP build in MySQL functions to allow for faster MySQL   *
 * data access and easy logging & debugging of MySQL        *
 * queries.                                                 *
 ************************************************************
 *  Copyright © 2005-2015 Artician Web Development Studios  *
 *  Eric Zhivalyuk - Henry Paradiz - Brandon Lis            *
 *  hparadiz@artician.com                                   *
 ************************************************************/

/**** CONSTANTS ****/
define('DATABASE_HOST', 'localhost');
define('DATABASE_USER', 'jplai');
define('DATABASE_PASS', 'H9PpFzV5w7EQRjde');
define('DATABASE_NAME', 'jplai');
define('DATABASE_PCON', true);

/*******************************************************************************
 *******************************************************************************
 ************
 ************  DATABASE CONNECTION
 ************
 *******************************************************************************
 ******************************************************************************/
//define('PRODUCTION', $_COOKIE['debug']?FALSE:TRUE);
define('PRODUCTION', FALSE);

$_DB_LINK = false;
$_QUERIES = array();
$_QUERY_HISTORY = array(); // Debug
$_DBO_CACHE = array();

class db_counter {
    public static $time = 0;
    public static $query = 0;
    public static $mysqltimes = array();
    public static $recordstimes = array();
    public static $mysql_strings = array();
    public static $mysql_backtraces = array();
    public static $enabled = FALSE;
    public static $records = 0;
    public static $enablebacktrace=TRUE;
    
    function enable() {
        self::$enabled = TRUE;
    }
    function disable() {
        self::$enabled = FALSE;
    }
    function start($string) {
        if (!self::$enabled) { return FALSE; }
        global $microtime;
        self::$query++;
        $query=self::$query;
        self::$mysql_strings['mysql_q_' . $query] = $string;
        if (self::$enablebacktrace) {
            ob_start();
            debug_print_backtrace();
            $debug = ob_get_contents();
            ob_end_clean();
            self::$mysql_backtraces['mysql_q_' . $query] = $debug;
        }
        $microtime->newStopWatch('mysql_q_' . $query);
        return $query;
    }
    function stop($which) {
        if (!self::$enabled) { return FALSE; }
        global $microtime;
        $microtime->stopStopWatch('mysql_q_' . $which);
        self::$mysqltimes[$which] = $microtime->returnInterval('mysql_q_' . $which);
        unset($microtime->start['mysql_q_' . $which]);
        unset($microtime->end['mysql_q_' . $which]);
    }
    function startr() {
        if (!self::$enabled) { return FALSE; }
        global $microtime;
        self::$records++;
        $records=self::$records;
        $microtime->newStopWatch('mysql_r_' . $records);
        return $records;
    }
    function stopr($which) {
        if (!self::$enabled) { return FALSE; }
        global $microtime;
        $microtime->stopStopWatch('mysql_r_' . $which);
        self::$recordstimes[$which] = $microtime->returnInterval('mysql_r_' . $which);
        unset($microtime->start['mysql_r_' . $which]);
        unset($microtime->end['mysql_r_' . $which]);
    }
    function display() {
        //if (!self::$enabled) { return FALSE; }
        $output = "<textarea style=\"width:90%; height: 200px;\" wrap=\"off\">\n";
        $total = 0;
        $totalr = 0;
        while(list($key,$value) = each(self::$mysqltimes)) {
            $total = ($total+$value);
            $output .= self::$mysql_strings['mysql_q_' . $key]." {$key} - {$value}\n";
            if (self::$enablebacktrace) {
                $output .= "|*********************BACKTRACE*******************************|\n";
                $output .= self::$mysql_backtraces['mysql_q_' . $key]."\n";
                $output .= "|*************************************************************|\n";
            }
        }
        while(list($key,$value) = each(self::$recordstimes)) {
            $totalr = ($totalr+$value);
        }
        $output .= "</textarea>";
        print '<b>MySQL</b><br>';
        print count(self::$mysqltimes)." MySQL Queries took {$total} seconds\n<br>";
        print count(self::$recordstimes)." MySQL Records took {$totalr} seconds\n<br>";
        print $output;
    }
}

function db_connect()
{
    global $_DB_LINK;

    if(DATABASE_PCON)
    {
        $_DB_LINK = mysql_pconnect(DATABASE_HOST, DATABASE_USER, DATABASE_PASS) or db_error("[Connection]");
    }
    else
    {
        $_DB_LINK = mysql_connect(DATABASE_HOST, DATABASE_USER, DATABASE_PASS) or db_error("[Connection]");
    }

    if($DB_LINK === false)
    {
        db_error('[Connect]');
    }

    mysql_select_db(DATABASE_NAME) or db_error('[Selection]');
}


/****************************************************************************
*****************************************************************************/
function db_error($query = '')
{
    if(!PRODUCTION)
    {
        ob_start();
        debug_print_backtrace();
        $debug = ob_get_contents();
        ob_end_clean();
        die("<b>Database Error.</b><br />query: $query<br />reported: " . mysql_error()."Backtrace: <pre>".$debug."</pre>");
    }
    else
    {
        die('<b>Database Error</b>');
    }
}


/****************************************************************************
*****************************************************************************/
function db_d($string, $qid = 0)
{
    die($string);
}


/****************************************************************************
*****************************************************************************/
function db_q($string, $qid = 0)
{
    global $_QUERIES;

    if(!PRODUCTION) $GLOBALS['_QUERY_HISTORY'][] = 'Q_R: ' . $string;
    if($GLOBALS['_DB_LINK']===false) db_connect();
    $counter=db_counter::start($string);
    if($_QUERIES[$qid] = mysql_query($string,$GLOBALS['_DB_LINK'])) {
        db_counter::stop($counter);
        return $_QUERIES[$qid];
    } else {
        db_counter::stop($counter);
        db_error($string);
        return $_QUERIES[$qid];
    }
}

/****************************************************************************
*****************************************************************************/
function db_n($string) {
    if(!PRODUCTION) $GLOBALS['_QUERY_HISTORY'][] = 'NOR: ' . $string;
    if($GLOBALS['_DB_LINK']===false) db_connect();
    $counter=db_counter::start($string);
    if($res = mysql_query($string,$GLOBALS['_DB_LINK']))
    {
        db_counter::stop($counter);
        return $res;
    } else {
        db_counter::stop($counter);
        db_error($string);
        return $res;
    }
}

/****************************************************************************
Running the db_ubq() function runs a Unbuffered Query, MySQL won't buffer/fetch any
results in memory so its less strain on MySQL. Note: After you run this command the next
MySQL query you do other then mysql_fetch_row or mysql_fetch_array will kill this result
query, So be very careful on how you write your scripts with this function, it could break it
all ... It's mostly for when you just want to run a query like an Update where there isn't a result.
- Brandon Lis
*****************************************************************************/
function db_ubq($string) {
    if(!PRODUCTION) $GLOBALS['_QUERY_HISTORY'][] = 'NOR: ' . $string;
    if($GLOBALS['_DB_LINK']===false) db_connect();
    $counter=db_counter::start($string);
    if($res = mysql_unbuffered_query($string))
    {
        db_counter::stop($counter);
        return true;
    } else {
        db_counter::stop($counter);
        db_error($string);
        return false;
    }
}
/****************************************************************************
*****************************************************************************/
function db_r($qid = 0)
{
    global $_QUERIES;
    if (!$_QUERIES[$qid]) { db_error(''); return false; }
    $counter=db_counter::startr();    
    $q=mysql_fetch_array($_QUERIES[$qid], MYSQL_ASSOC);
    db_counter::stopr($counter);
    return $q;
}


/****************************************************************************
*****************************************************************************/
function db_done($qid = 0)
{
    global $_QUERIES;

    $outcome = mysql_free_result($_QUERIES[$qid]);
    unset($_QUERIES[$qid]);

    return $outcome;
}

/****************************************************************************
***************************************************************************/
function db_all($string, $free = true)
{

    $results = array();

    db_q($string, '_INTERNAL_DB_ALL');
    $counter=db_counter::startr();
    while($result = db_r('_INTERNAL_DB_ALL'))
    {
        $results[] = $result;
    }

    if($free) {
        db_done('_INTERNAL_DB_ALL');
    }
    db_counter::stopr($counter);
    return $results;
}

/****************************************************************************
***************************************************************************/
function db_all_values($string, $key = 'value', $free = true)
{
    $results = array();

    db_q($string, '_INTERNAL_DB_ALL_VALUES');
    $counter=db_counter::startr();
    while($result = db_r('_INTERNAL_DB_ALL_VALUES'))
    {
        $results[] = $result[$key];
    }

    if($free) {
        db_done('_INTERNAL_DB_ALL_VALUES');
    }
    db_counter::stopr($counter);
    return $results;
}


/****************************************************************************
***************************************************************************/
function db_o($string, $usecache = false)
{
    if($GLOBALS['_DB_LINK']===false) db_connect();
    if($usecache && array_key_exists($string, $GLOBALS['_DBO_CACHE']))
    {
        return $GLOBALS['_DBO_CACHE'][$string];
    }
    else
    {
        if(!PRODUCTION) $GLOBALS['_QUERY_HISTORY'][] =  'ONE('. var_export($usecache, true) .'): ' . $string;
        $counter=db_counter::start($string);
        $dbo_res = mysql_query($string,$GLOBALS['_DB_LINK']) or db_error($string);
        db_counter::stop($counter);
        $counter=db_counter::startr();
        $result = mysql_fetch_array($dbo_res, MYSQL_ASSOC);
        db_counter::stopr($counter);
        mysql_free_result($dbo_res);

        if($usecache)
        {
            $GLOBALS['_DBO_CACHE'][$string] = $result;
        }

        return $result;
    }

}


/****************************************************************************
***************************************************************************/
function db_ov($string, $key = 'value')
{
    $result = db_o($string);
    return $result[$key];
}

/****************************************************************************
*****************************************************************************/
function db_nr($qid = 0)
{
    global $_QUERIES;
    return mysql_num_rows($_QUERIES[$qid]);
}


/****************************************************************************
*****************************************************************************/
function db_ar()
{
    global $_DB_LINK;

    return mysql_affected_rows($_DB_LINK);
}


/****************************************************************************
***************************************************************************/
function db_escape($string, $cutoff = 255)
{
    if($GLOBALS['_DB_LINK']===false) db_connect();

    if($cutoff)
        return mysql_real_escape_string(substr($string, 0, $cutoff),$GLOBALS['_DB_LINK']);
    else
        return mysql_real_escape_string($string,$GLOBALS['_DB_LINK']);
}


/****************************************************************************
***************************************************************************/
function db_id()
{
    global $_DB_LINK;

    return mysql_insert_id($_DB_LINK);
}
?>
