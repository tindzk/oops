#!/usr/bin/env python
import fnmatch, os, sys, subprocess

def run(cmd):
	try:
		p = subprocess.Popen(cmd)
		(out, err) = p.communicate()

		if out:
			print('out: %s' % out.decode(sys.stdout.encoding or 'iso8859-1'))

		if err:
			print('err: %s' % err.decode(sys.stdout.encoding or 'iso8859-1'))

		return p.returncode
	except OSError:
		return -1

def matchFiles(path, pattern):
	paths = []
	for root, dirnames, filenames in os.walk(path):
		for filename in fnmatch.filter(filenames, pattern):
			paths.append(os.path.join(root, filename))
	return paths

jarFiles = matchFiles('libs/', '*.jar')

for app in ["OOPSC", "OOPSVM"]:
	if not os.path.exists("build/" + app):
		os.makedirs("build/" + app)

	srcFiles = matchFiles(app + '/src/', '*.java')

	for file in srcFiles:
		if "TestSuite.java" in file:
			srcFiles.remove(file)
			break

	cmdCompile = ["/usr/bin/javac", "-cp", ":".join(jarFiles), "-d", "build/" + app] + srcFiles
	print(" ".join(cmdCompile))
	run(cmdCompile)

	classFiles = matchFiles('build/' + app, '*.class')
	cmdPackage = ["/usr/bin/jar", "cvmfe", "Manifest.txt", app.lower() + ".jar", app, "-C", "build/" + app, app + ".class"] + classFiles
	print(" ".join(cmdPackage))
	run(cmdPackage)
