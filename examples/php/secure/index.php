<?php
$headers = getallheaders();
foreach ($headers as $name => $value) {
    echo "$name: $value\n";
}
?>
