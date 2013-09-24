#!/bin/sh
COMP="java -jar oopsc.jar"
VM="java -jar oopsvm.jar"

function test_file() {
	local dir=`dirname $1`
	local file=`basename -s .oops $1`
	mkdir "/tmp/oops_test" 2> /dev/null
	printf "%-40s" "Running test on $dir/$file"
	$COMP "$dir/$file.oops" "/tmp/oops_test/$file.comp" > "/tmp/oops_test/$file.out"
	if [ $? -eq 0 ]; then
		echo -en "\e[49;32mCompilation: Success\t\e[0m"
		echo abc | $VM "/tmp/oops_test/$file.comp" > "/tmp/oops_test/$file.out";
		echo xyz | $VM "/tmp/oops_test/$file.comp" >> "/tmp/oops_test/$file.out";
		diff "/tmp/oops_test/$file.out" "$dir/$file.out" > /dev/null;
		if [ $? -eq 0 ]; then
			echo -e "\e[49;32mRun: Success\e[0m"
			rm "/tmp/oops_test/$file"*;
		else
			echo -e "\e[49;31mRun: Failed\e[0m"
		fi;
	else
		diff "/tmp/oops_test/$file.out" "$dir/$file.out" > /dev/null;
		if [ $? -eq 0 ]; then
			echo -e "\e[49;32mCompilation: Success\t(SE Test)\e[0m"
			rm "/tmp/oops_test/$file"*;
		else
			echo -e "\e[49;31mCompilation: Failed\e[0m"
		fi;
	fi;
}

function test_files() {
	for fi in $@; do
		test_file "$fi";
	done;
}

test_files tests/$1*.oops